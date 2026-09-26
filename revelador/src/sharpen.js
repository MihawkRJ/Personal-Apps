/* ================= Correção de tremido e desfoque =================
   Trabalha só na luminância: a diferença de brilho recuperada é somada
   igualmente a R, G e B, então matiz e saturação da foto não mudam.
   Tamanhos de borrão são guardados como fração do lado maior da foto,
   assim a prévia e a exportação em tamanho cheio ficam iguais. */
const SHARP_WORK_MAX = 1400;   // resolução máxima de trabalho da deconvolução
var SHARP_ITERS = 10, SHARP_RCLAMP = [.5,2], SHARP_MARGIN = 0;

function lumaOf(d,n){const Y=new Float32Array(n);for(let i=0,p=0;i<n;i++,p+=4)Y[i]=(.299*d[p]+.587*d[p+1]+.114*d[p+2])/255;return Y;}

// Redução por média de área (usada para análise e para a resolução de trabalho)
function downsampleArea(src,w,h,nw,nh){
  const out=new Float32Array(nw*nh), sx=w/nw, sy=h/nh;
  for(let y=0;y<nh;y++){const y0=Math.floor(y*sy), y1=Math.max(y0+1,Math.floor((y+1)*sy));
    for(let x=0;x<nw;x++){const x0=Math.floor(x*sx), x1=Math.max(x0+1,Math.floor((x+1)*sx));let s=0,c=0;
      for(let yy=y0;yy<y1&&yy<h;yy++){let p=yy*w+x0;for(let xx=x0;xx<x1&&xx<w;xx++,p++){s+=src[p];c++;}}
      out[y*nw+x]=c?s/c:0;}}
  return out;
}
function sampleBil(Y,w,h,x,y){
  if(x<0)x=0;if(y<0)y=0;if(x>w-1.001)x=w-1.001;if(y>h-1.001)y=h-1.001;
  const x0=x|0,y0=y|0,fx=x-x0,fy=y-y0,p=y0*w+x0;
  return (Y[p]*(1-fx)+Y[p+1]*fx)*(1-fy)+(Y[p+w]*(1-fx)+Y[p+w+1]*fx)*fy;
}

/* ---------- Análise: mede a largura das bordas em 12 direções ----------
   Tremido alarga as bordas só na direção do movimento; foco alarga em todas. */
function analyzeBlur(rgba,w,h){
  const long=Math.max(w,h), s=Math.min(1,800/long), aw=Math.max(8,Math.round(w*s)), ah=Math.max(8,Math.round(h*s));
  const Y=downsampleArea(lumaOf(rgba,w*h),w,h,aw,ah);
  const gx=new Float32Array(aw*ah), gy=new Float32Array(aw*ah), mag=new Float32Array(aw*ah);
  for(let y=1;y<ah-1;y++)for(let x=1;x<aw-1;x++){const p=y*aw+x;
    const a=Y[p-aw-1],b=Y[p-aw],c=Y[p-aw+1],d=Y[p-1],f=Y[p+1],g=Y[p+aw-1],hh=Y[p+aw],i=Y[p+aw+1];
    const X=(c+2*f+i)-(a+2*d+g), Z=(g+2*hh+i)-(a+2*b+c); gx[p]=X;gy[p]=Z;mag[p]=Math.hypot(X,Z);}
  // limiar: 1,5% mais fortes
  const hist=new Uint32Array(512);let mx=0;for(let i=0;i<mag.length;i++)if(mag[i]>mx)mx=mag[i];
  if(mx<1e-3)return {type:"sharp",angle:0,frac:0,desc:"Sem bordas suficientes para analisar."};
  for(let i=0;i<mag.length;i++)hist[Math.min(511,(mag[i]/mx*511)|0)]++;
  let acc=0,thr=mx;for(let b=511;b>=0;b--){acc+=hist[b];if(acc>mag.length*.10){thr=b/511*mx;break;}}
  const BINS=12, widths=Array.from({length:BINS},()=>[]);
  const R=24, step=.5;
  let cnt=0;
  for(let y=R;y<ah-R;y+=2)for(let x=R;x<aw-R;x+=2){
    const p=y*aw+x, m=mag[p]; if(m<thr)continue;
    const ux=gx[p]/m, uy=gy[p]/m;
    // máximo local ao longo do gradiente
    if(sampleBil(mag,aw,ah,x+ux,y+uy)>m||sampleBil(mag,aw,ah,x-ux,y-uy)>m)continue;
    const raw=[];for(let t=-R;t<=R;t+=step)raw.push(sampleBil(Y,aw,ah,x+ux*t,y+uy*t));
    const prof=raw.map((v,k)=>(raw[Math.max(0,k-1)]+2*v+raw[Math.min(raw.length-1,k+1)])/4);
    const c=(prof.length-1)>>1;
    // anda do centro para os dois lados até o perfil parar de subir/descer
    let lo=c,hi=c;while(lo>0&&prof[lo-1]<=prof[lo]+.006)lo--; while(hi<prof.length-1&&prof[hi+1]>=prof[hi]-.006)hi++;
    const vlo=prof[lo], vhi=prof[hi], dv=vhi-vlo; if(dv<.1)continue;
    let a10=lo,a90=hi;for(let k=lo;k<=hi;k++){if(prof[k]>=vlo+.1*dv){a10=k;break;}}
    for(let k=lo;k<=hi;k++){if(prof[k]>=vlo+.9*dv){a90=k;break;}}
    const wdt=(a90-a10)*step; if(wdt<=0)continue;
    let ang=Math.atan2(uy,ux)*180/Math.PI; if(ang<0)ang+=180; if(ang>=180)ang-=180;
    widths[Math.floor(ang/15)%BINS].push(wdt); if(++cnt>6000)break;
  }
  const med=a=>{if(a.length<12)return null;const s=[...a].sort((p,q)=>p-q);return s[s.length>>1];};
  const m=widths.map(med), valid=m.map((v,i)=>v==null?null:{v,i}).filter(Boolean);
  if(valid.length<3)return {type:"sharp",angle:0,frac:0,desc:"Poucas bordas nítidas para medir; aplicando só realce leve."};
  // suaviza bins vizinhos (circular)
  const sm=m.map((v,i)=>{if(v==null)return null;let s=v*2,c=2;[-1,1].forEach(o=>{const u=m[(i+o+BINS)%BINS];if(u!=null){s+=u;c++;}});return s/c;});
  let iMax=-1,iMin=-1;sm.forEach((v,i)=>{if(v==null)return;if(iMax<0||v>sm[iMax])iMax=i;if(iMin<0||v<sm[iMin])iMin=i;});
  const wMax=sm[iMax], wMin=sm[iMin], base=1.6;
  const toFrac=px=>px/Math.max(aw,ah);
  if(wMax-wMin>Math.max(1.2,.3*wMin)){
    const L=Math.max(1,1.15*((wMax-wMin)/.8+Math.max(0,wMin-base)*.3));
    const angle=(iMax*15+7.5)%180;
    return {type:"motion",angle,frac:toFrac(L),defocusFrac:toFrac(Math.max(0,wMin-base)/2.56),
      desc:`Tremido detectado em ${dirName(angle)} (${Math.round(angle)}°), rastro de ~${Math.round(toFrac(L)*Math.max(w,h))} px.`};
  }
  const sig=1.3*Math.max(0,wMin-base)/2.56;
  if(sig<.35)return {type:"sharp",angle:0,frac:0,desc:"A foto já está nítida; aplicando só realce leve de detalhes."};
  return {type:"defocus",angle:0,frac:toFrac(sig),desc:`Desfoque (fora de foco) detectado, raio de ~${Math.round(toFrac(sig)*Math.max(w,h)*2.5)} px.`};
}
function dirName(a){if(a<22.5||a>=157.5)return"direção horizontal";if(a<67.5)return"diagonal ↘";if(a<112.5)return"direção vertical";return"diagonal ↗";}

/* ---------- Núcleos (PSF) ---------- */
function motionKernel(L,angleDeg,W){
  const map=new Map(), ca=Math.cos(angleDeg*Math.PI/180), sa=Math.sin(angleDeg*Math.PI/180);
  let R=0;
  for(let t=-L/2;t<=L/2+1e-6;t+=.25){const x=t*ca,y=t*sa,x0=Math.floor(x),y0=Math.floor(y),fx=x-x0,fy=y-y0;
    [[0,0,(1-fx)*(1-fy)],[1,0,fx*(1-fy)],[0,1,(1-fx)*fy],[1,1,fx*fy]].forEach(([dx,dy,wt])=>{if(wt<=0)return;
      const k=(x0+dx)+","+(y0+dy);map.set(k,(map.get(k)||0)+wt);R=Math.max(R,Math.abs(x0+dx),Math.abs(y0+dy));});}
  let sum=0;map.forEach(v=>sum+=v);
  const PW=W+2*R, offs=[], wts=[];
  map.forEach((v,k)=>{const [dx,dy]=k.split(",").map(Number);offs.push(dy*PW+dx);wts.push(v/sum);});
  return {R,offs:Int32Array.from(offs),wts:Float32Array.from(wts),PW};
}
function convSparse(src,w,h,K,dst,P){
  const R=K.R,PW=K.PW,PH=h+2*R;
  for(let y=0;y<PH;y++){const sy=Math.min(h-1,Math.max(0,y-R));for(let x=0;x<PW;x++){const sx=Math.min(w-1,Math.max(0,x-R));P[y*PW+x]=src[sy*w+sx];}}
  const o=K.offs,wt=K.wts,n=o.length;
  for(let y=0;y<h;y++){let base=(y+R)*PW+R,q=y*w;for(let x=0;x<w;x++,base++,q++){let s=0;for(let k=0;k<n;k++)s+=P[base+o[k]]*wt[k];dst[q]=s;}}
}
function gaussTaps(sig){const r=Math.max(1,Math.ceil(sig*3)),t=new Float32Array(2*r+1);let s=0;for(let i=-r;i<=r;i++){t[i+r]=Math.exp(-i*i/(2*sig*sig));s+=t[i+r];}for(let i=0;i<t.length;i++)t[i]/=s;return t;}
function convGauss(src,w,h,taps,dst,tmp){
  const r=(taps.length-1)>>1;
  for(let y=0;y<h;y++){const row=y*w;for(let x=0;x<w;x++){let s=0;for(let k=-r;k<=r;k++){let xx=x+k;xx=xx<0?0:xx>=w?w-1:xx;s+=src[row+xx]*taps[k+r];}tmp[row+x]=s;}}
  for(let y=0;y<h;y++){for(let x=0;x<w;x++){let s=0;for(let k=-r;k<=r;k++){let yy=y+k;yy=yy<0?0:yy>=h?h-1:yy;s+=tmp[yy*w+x]*taps[k+r];}dst[y*w+x]=s;}}
}
// mínimo/máximo local separável (anti-halo)
function localMinMax(src,w,h,r){
  const mn=new Float32Array(w*h),mx=new Float32Array(w*h),t1=new Float32Array(w*h),t2=new Float32Array(w*h);
  for(let y=0;y<h;y++){const row=y*w;for(let x=0;x<w;x++){let a=1e9,b=-1e9;for(let k=Math.max(0,x-r);k<=Math.min(w-1,x+r);k++){const v=src[row+k];if(v<a)a=v;if(v>b)b=v;}t1[row+x]=a;t2[row+x]=b;}}
  for(let x=0;x<w;x++){for(let y=0;y<h;y++){let a=1e9,b=-1e9;for(let k=Math.max(0,y-r);k<=Math.min(h-1,y+r);k++){const p=k*w+x;if(t1[p]<a)a=t1[p];if(t2[p]>b)b=t2[p];}mn[y*w+x]=a;mx[y*w+x]=b;}}
  return [mn,mx];
}
const tick=()=>new Promise(r=>setTimeout(r,0));

/* ---------- Aplicação ----------
   cfg: {type:'motion'|'defocus'|'sharp', angle, frac, defocusFrac, amount}
   Altera rgba no lugar. onProgress(0..1) opcional. */
async function sharpenRGBA(rgba,w,h,cfg,onProgress){
  const n=w*h, long=Math.max(w,h), Y=lumaOf(rgba,n), amount=cfg.amount;
  const delta=new Float32Array(n);
  if(cfg.type==="motion"||cfg.type==="defocus"){
    // resolução de trabalho: no máximo SHARP_WORK_MAX e rastro de até 14 px
    const blurPx=cfg.frac*long*(cfg.type==="defocus"?2.5:1);
    let ws=Math.min(1,SHARP_WORK_MAX/long); if(blurPx*ws>12)ws=12/blurPx;
    const ww=Math.max(8,Math.round(w*ws)), wh=Math.max(8,Math.round(h*ws)), wl=Math.max(ww,wh);
    const Yw=ws<1?downsampleArea(Y,w,h,ww,wh):Y.slice();
    const N=ww*wh, off=.02, obs=new Float32Array(N); for(let i=0;i<N;i++)obs[i]=Yw[i]+off;
    const est=obs.slice(), bl=new Float32Array(N), ratio=new Float32Array(N), corr=new Float32Array(N);
    let conv, rr;
    if(cfg.type==="motion"){
      const L=Math.max(1.2,cfg.frac*wl), K=motionKernel(L,cfg.angle,ww), P=new Float32Array((ww+2*K.R)*(wh+2*K.R));
      conv=(s,d)=>convSparse(s,ww,wh,K,d,P); rr=Math.ceil(L/2)+1;
    }else{
      const sig=Math.max(.5,cfg.frac*wl), taps=gaussTaps(sig), tmp=new Float32Array(N);
      conv=(s,d)=>convGauss(s,ww,wh,taps,d,tmp); rr=Math.ceil(sig*2)+1;
    }
    for(let it=0;it<SHARP_ITERS;it++){
      conv(est,bl);
      for(let i=0;i<N;i++){let r=obs[i]/Math.max(bl[i],1e-4);ratio[i]=r<SHARP_RCLAMP[0]?SHARP_RCLAMP[0]:r>SHARP_RCLAMP[1]?SHARP_RCLAMP[1]:r;}
      conv(ratio,corr); // núcleo simétrico: o adjunto é ele mesmo
      for(let i=0;i<N;i++){let v=est[i]*corr[i];est[i]=v<0?0:v>1.3?1.3:v;}
      if(onProgress)onProgress((it+1)/(SHARP_ITERS+2)); await tick();
    }
    const [mn,mx]=localMinMax(Yw,ww,wh,rr), m=SHARP_MARGIN, dW=new Float32Array(N);
    const edge=Math.max(2,rr*2);
    for(let y=0;y<wh;y++)for(let x=0;x<ww;x++){const i=y*ww+x;let v=est[i]-off;const lo=mn[i]-m,hi=mx[i]+m;v=v<lo?lo:v>hi?hi:v;
      const e=Math.min(x,y,ww-1-x,wh-1-y), fade=e>=edge?1:e/edge; dW[i]=(v-Yw[i])*amount*fade;}
    if(ws<1){ // leva a correção de volta à resolução cheia
      const sx=ww/w, sy=wh/h;
      for(let y=0;y<h;y++){const fy=(y+.5)*sy-.5;for(let x=0;x<w;x++)delta[y*w+x]=sampleBil(dW,ww,wh,(x+.5)*sx-.5,fy);}
    }else delta.set(dW);
  }
  // realce fino de detalhes com limiar contra ruído (sempre, na resolução final)
  const sig=Math.max(.7,long/2600), taps=gaussTaps(sig), Y1=new Float32Array(n), B=new Float32Array(n), tmp=new Float32Array(n);
  for(let i=0;i<n;i++)Y1[i]=Y[i]+delta[i];
  convGauss(Y1,w,h,taps,B,tmp);
  const dAmt=(cfg.type==="sharp"?.9:.45)*amount, th=.018;
  for(let i=0;i<n;i++){let d=Y1[i]-B[i];const a=Math.abs(d);d=a<th?0:(d>0?a-th:th-a);delta[i]+=d*dAmt;}
  if(onProgress)onProgress(1);
  for(let i=0,p=0;i<n;i++,p+=4){const dd=delta[i]*255;if(dd===0)continue;
    rgba[p]=rgba[p]+dd; rgba[p+1]=rgba[p+1]+dd; rgba[p+2]=rgba[p+2]+dd;} // Uint8ClampedArray satura sozinho
}
