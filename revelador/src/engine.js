/* ---------------- Motor de cor ---------------- */
const NUMK=["exposure","temp","tint","contrast","highlights","shadows","whites","blacks","sat","vib","mono","vignette","grain"];
const I3=[[1,0,0],[0,1,0],[0,0,1]];
function neutral(){const o={};NUMK.forEach(k=>o[k]=0);o.st=[0,0,0];o.ht=[0,0,0];o.matrix=null;return o;}
function mmul(a,b){return a.map((r,i)=>[0,1,2].map(j=>r[0]*b[0][j]+r[1]*b[1][j]+r[2]*b[2][j]));}
function addP(acc,p,s){
  NUMK.forEach(k=>{if(p[k])acc[k]+=p[k]*s;});
  if(p.st)for(let i=0;i<3;i++)acc.st[i]+=p.st[i]*s;
  if(p.ht)for(let i=0;i<3;i++)acc.ht[i]+=p.ht[i]*s;
  if(p.matrix){const m=I3.map((r,i)=>r.map((v,j)=>v+(p.matrix[i][j]-v)*s));acc.matrix=acc.matrix?mmul(m,acc.matrix):m;}
}
function buildParams(){
  const P=neutral(), s=S.intensity;
  [SCENES.find(x=>x.id===S.scene),GRADES.find(x=>x.id===S.grade),CAMS.find(x=>x.id===S.cam)].forEach(x=>x&&addP(P,x.p,s));
  FINE.forEach(f=>{P[f.k]+=S.fine[f.k]*f.scale;});
  P.mono=Math.min(1,P.mono); P.vignette=Math.max(0,Math.min(1.2,P.vignette)); P.grain=Math.max(0,P.grain);
  P.whites=Math.max(0,Math.min(1,P.whites)); P.blacks=Math.max(0,Math.min(.3,P.blacks));
  return P;
}
const lin=x=>x<=0?0:Math.pow(x,2.2), gam=x=>x<=0?0:Math.pow(x,1/2.2);
const luma=(r,g,b)=>.2126*r+.7152*g+.0722*b;
const clamp01=x=>x<0?0:x>1?1:x;
function hueSat(r,g,b){const mx=Math.max(r,g,b),mn=Math.min(r,g,b),d=mx-mn;let h=0;
  if(d>1e-6){if(mx===r)h=((g-b)/d)%6;else if(mx===g)h=(b-r)/d+2;else h=(r-g)/d+4;h*=60;if(h<0)h+=360;}
  return [h, mx>0?d/mx:0, mx];}

function makeTransform(P,opt,st){
  const ev=Math.pow(2,P.exposure), M=P.matrix;
  const wr=1+.22*P.temp, wb=1-.22*P.temp, wg=1-.12*P.tint;
  const knee=1-.55*P.whites;
  const k=Math.max(.35,1+P.contrast*1.1);
  const useAuto=opt.auto&&st;
  const gains=useAuto?st.gains:[1,1,1], bp=useAuto?st.bp:0, wp=useAuto?st.wp:1;
  const shoulder=x=>{ if(x<=knee)return x; if(knee>=1)return 1; return knee+(1-knee)*(1-Math.exp(-(x-knee)/(1-knee))); };
  const curve=x=>x<.5?.5*Math.pow(2*x,k):1-.5*Math.pow(2-2*x,k);
  const hasTone=P.shadows!==0||P.highlights!==0, hasCurve=Math.abs(k-1)>1e-4;
  const satF=1+P.sat;
  return (r0,g0,b0)=>{
    const oL=luma(r0,g0,b0);
    let r=r0,g=g0,b=b0;
    if(useAuto){const s=1/(wp-bp);r=(r-bp)*s;g=(g-bp)*s;b=(b-bp)*s;}
    let R=lin(r)*gains[0]*wr*ev, G=lin(g)*gains[1]*wg*ev, B=lin(b)*gains[2]*wb*ev;
    if(M){const a=M[0][0]*R+M[0][1]*G+M[0][2]*B, c=M[1][0]*R+M[1][1]*G+M[1][2]*B, d=M[2][0]*R+M[2][1]*G+M[2][2]*B;R=a;G=c;B=d;}
    R=gam(shoulder(Math.max(0,R)));G=gam(shoulder(Math.max(0,G)));B=gam(shoulder(Math.max(0,B)));
    r=clamp01(R);g=clamp01(G);b=clamp01(B);
    if(hasTone){
      const L=luma(r,g,b);
      const dL=P.shadows*.55*Math.pow(1-L,3)*Math.pow(L,.6)+P.highlights*.55*L*L*L*Math.pow(1-L,.6);
      if(L>1e-4){const f=Math.max(0,(L+dL)/L);r*=f;g*=f;b*=f;}else{r+=dL;g+=dL;b+=dL;}
      r=clamp01(r);g=clamp01(g);b=clamp01(b);
    }
    if(hasCurve){r=curve(r);g=curve(g);b=curve(b);}
    let L=luma(r,g,b);
    if(P.sat!==0||P.vib!==0){
      const mx=Math.max(r,g,b),mn=Math.min(r,g,b),s=mx-mn;
      const f=Math.max(0,satF*(1+P.vib*(1-s)*(P.vib>0?1.1:1)));
      r=L+(r-L)*f;g=L+(g-L)*f;b=L+(b-L)*f;
    }
    if(P.st[0]||P.st[1]||P.st[2]||P.ht[0]||P.ht[1]||P.ht[2]){
      const ws=(1-L)*(1-L), wh=L*L;
      let o0=P.st[0]*ws+P.ht[0]*wh,o1=P.st[1]*ws+P.ht[1]*wh,o2=P.st[2]*ws+P.ht[2]*wh;
      const d=luma(o0,o1,o2); r+=o0-d; g+=o1-d; b+=o2-d;
    }
    if(P.blacks>0){const bl=P.blacks;r=bl+r*(1-bl);g=bl+g*(1-bl);b=bl+b*(1-bl);}
    if(P.mono>0){const m=clamp01(.3*r+.59*g+.11*b);r+= (m-r)*P.mono;g+=(m-g)*P.mono;b+=(m-b)*P.mono;}
    r=clamp01(r);g=clamp01(g);b=clamp01(b);
    if(opt.keepLuma){const nL=luma(r,g,b);
      if(nL>1e-4){const f=oL/nL;r*=f;g*=f;b*=f;}else{r=g=b=oL;}
      const mx=Math.max(r,g,b); if(mx>1){r/=mx;g/=mx;b/=mx;}
    }
    if(opt.skin){
      const [h,sat,v]=hueSat(r0,g0,b0);
      let w=0;
      if(h>=2&&h<=50&&sat>.12&&sat<.7&&v>.2){
        const wh=h<10?(h-2)/8:h>38?(50-h)/12:1, ws=sat<.2?(sat-.12)/.08:sat>.58?(.7-sat)/.12:1, wv=v<.3?(v-.2)/.1:1;
        w=Math.max(0,Math.min(1,wh*ws*wv))*.8;
      }
      if(w>0){const nL=luma(r,g,b), f=oL>1e-4?nL/oL:1;
        r+=(clamp01(r0*f)-r)*w;g+=(clamp01(g0*f)-g)*w;b+=(clamp01(b0*f)-b)*w;}
    }
    return [clamp01(r),clamp01(g),clamp01(b)];
  };
}
const N=33;
function buildLUT(fn){
  const lut=new Float32Array(N*N*N*3); let i=0;
  for(let b=0;b<N;b++)for(let g=0;g<N;g++)for(let r=0;r<N;r++){const o=fn(r/(N-1),g/(N-1),b/(N-1));lut[i++]=o[0];lut[i++]=o[1];lut[i++]=o[2];}
  return lut;
}
function hash(x,y,s){let h=(x*374761393+y*668265263+s*2147483647)|0;h=Math.imul(h^(h>>>13),1274126177);return ((h^(h>>>16))>>>0)/4294967296;}
// Aplica LUT + efeitos espaciais (vinheta, grão) nas linhas [y0,y1)
function applyRows(d,o,lut,w,h,P,y0,y1){
  const n1=N-1, N2=N*N, vig=P.vignette, grain=P.grain*.11;
  const cx=w/2, cy=h/2, rmax=Math.sqrt(cx*cx+cy*cy), gsz=Math.max(1,Math.round(Math.max(w,h)/1800));
  for(let y=y0;y<y1;y++){
    let p=y*w*4;
    for(let x=0;x<w;x++,p+=4){
      const fr=d[p]/255*n1, fg=d[p+1]/255*n1, fb=d[p+2]/255*n1;
      let r0=fr|0,g0=fg|0,b0=fb|0; if(r0>=n1)r0=n1-1; if(g0>=n1)g0=n1-1; if(b0>=n1)b0=n1-1;
      const dr=fr-r0,dg=fg-g0,db=fb-b0;
      const i000=(b0*N2+g0*N+r0)*3, i100=i000+3, i010=i000+N*3, i110=i010+3, i001=i000+N2*3, i101=i001+3, i011=i001+N*3, i111=i011+3;
      const w000=(1-dr)*(1-dg)*(1-db), w100=dr*(1-dg)*(1-db), w010=(1-dr)*dg*(1-db), w110=dr*dg*(1-db),
            w001=(1-dr)*(1-dg)*db, w101=dr*(1-dg)*db, w011=(1-dr)*dg*db, w111=dr*dg*db;
      let R=lut[i000]*w000+lut[i100]*w100+lut[i010]*w010+lut[i110]*w110+lut[i001]*w001+lut[i101]*w101+lut[i011]*w011+lut[i111]*w111;
      let G=lut[i000+1]*w000+lut[i100+1]*w100+lut[i010+1]*w010+lut[i110+1]*w110+lut[i001+1]*w001+lut[i101+1]*w101+lut[i011+1]*w011+lut[i111+1]*w111;
      let B=lut[i000+2]*w000+lut[i100+2]*w100+lut[i010+2]*w010+lut[i110+2]*w110+lut[i001+2]*w001+lut[i101+2]*w101+lut[i011+2]*w011+lut[i111+2]*w111;
      if(vig>0){const dx=x-cx,dy=y-cy,rr=Math.sqrt(dx*dx+dy*dy)/rmax;
        let t=(rr-.35)/.7; t=t<0?0:t>1?1:t; const f=1-vig*.75*t*t*(3-2*t); R*=f;G*=f;B*=f;}
      if(grain>0){const L=.2126*R+.7152*G+.0722*B, n=(hash((x/gsz)|0,(y/gsz)|0,7)-.5)*grain*(1-Math.abs(2*L-1)*.5);R+=n;G+=n;B+=n;}
      o[p]=R*255+.5; o[p+1]=G*255+.5; o[p+2]=B*255+.5; o[p+3]=d[p+3];
    }
  }
}
function computeStats(data){
  const d=data.data, hist=new Uint32Array(256); let sr=0,sg=0,sb=0,n=0;
  for(let p=0;p<d.length;p+=16){const r=d[p],g=d[p+1],b=d[p+2];const L=Math.round(luma(r,g,b));hist[L]++;
    if(L>12&&L<245){sr+=lin(r/255);sg+=lin(g/255);sb+=lin(b/255);n++;}}
  let tot=0;for(const v of hist)tot+=v;
  let acc=0,lo=0,hi=255;for(let i=0;i<256;i++){acc+=hist[i];if(acc>=tot*.005){lo=i;break;}}
  acc=0;for(let i=255;i>=0;i--){acc+=hist[i];if(acc>=tot*.005){hi=i;break;}}
  const bp=Math.min(.12,lo/255)*.8, wp=Math.max(.75,hi/255)+(1-Math.max(.75,hi/255))*.2;
  let gains=[1,1,1];
  if(n>0){const mr=sr/n,mg=sg/n,mb=sb/n,avg=(mr+mg+mb)/3;
    gains=[mr,mg,mb].map(m=>{const gn=Math.pow(avg/Math.max(m,1e-4),.5);return Math.max(.82,Math.min(1.22,gn));});}
  return {bp,wp,gains};
}

