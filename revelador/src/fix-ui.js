/* ---------------- Correção de tremido/desfoque: estado e controles ----------------
   Usa os elementos: fixOn, fixDiag, fixModes (botões data-mode), fixAmt, fixAng, fixLen,
   oFixAmt, oFixAng, oFixLen, fixAngRow, fixLenRow, fixBusy. */
const FX={on:false,mode:"auto",amount:1,angle:0,len:0,an:null};
const FIX_LEN_MAX=.03; // 3% do lado maior
let baseData=null, fixToken=0, fixTimer=null;
function fixType(){return FX.mode==="auto"?(FX.an?FX.an.type:"sharp"):FX.mode;}
function fixCfg(){if(!FX.on)return null;const t=fixType();return {type:t,angle:FX.angle,frac:t==="sharp"?0:FX.len,amount:FX.amount};}
function srcLong(){return src?Math.max(src.naturalWidth||src.width,src.naturalHeight||src.height):1;}
function runAnalysis(){
  if(!origData)return; FX.an=analyzeBlur(origData.data,W,H);
  if(FX.an.type!=="sharp"){FX.angle=FX.an.angle;FX.len=FX.an.frac;}
  else if(!FX.len){FX.len=.004;}
}
async function updateFix(){
  const tok=++fixToken; if(!origData)return;
  const cfg=fixCfg();
  if(!cfg){baseData=null;$("fixBusy").hidden=true;schedule();return;}
  $("fixBusy").hidden=false; $("fixBusy").textContent="Aplicando correção…";
  const buf=new Uint8ClampedArray(origData.data);
  await sharpenRGBA(buf,W,H,cfg,p=>{if(tok===fixToken)$("fixBusy").textContent=`Aplicando correção… ${Math.round(p*100)}%`;});
  if(tok!==fixToken)return;
  baseData=new ImageData(buf,W,H); $("fixBusy").hidden=true; schedule();
}
function scheduleFix(){clearTimeout(fixTimer);fixTimer=setTimeout(updateFix,180);}
function syncFixUI(){
  $("fixOn").setAttribute("aria-pressed",FX.on);
  $("fixOn").querySelector("b").textContent=FX.on?"Correção de nitidez ligada":"Corrigir foto tremida ou desfocada";
  document.querySelectorAll("#fixModes button").forEach(b=>b.setAttribute("aria-pressed",b.dataset.mode===FX.mode));
  const t=fixType();
  $("fixDiag").textContent=FX.an?FX.an.desc:"Toque para analisar a foto e corrigir automaticamente.";
  $("fixAmt").value=Math.round(FX.amount*100); $("oFixAmt").textContent=Math.round(FX.amount*100)+"%";
  $("fixAng").value=Math.round(FX.angle); $("oFixAng").textContent=Math.round(FX.angle)+"°";
  $("fixLen").value=Math.round(FX.len/FIX_LEN_MAX*100); $("oFixLen").textContent="~"+Math.round(FX.len*srcLong())+" px";
  $("fixAngRow").hidden=!FX.on||t!=="motion"; $("fixLenRow").hidden=!FX.on||t==="sharp";
  $("fixModes").hidden=!FX.on; $("fixAmtRow").hidden=!FX.on;
}
$("fixOn").onclick=()=>{FX.on=!FX.on; if(FX.on&&!FX.an)runAnalysis(); syncFixUI(); scheduleFix();};
document.querySelectorAll("#fixModes button").forEach(b=>b.onclick=()=>{FX.mode=b.dataset.mode;
  if(FX.mode!=="sharp"&&!FX.len)FX.len=.006; syncFixUI(); scheduleFix();});
$("fixAmt").addEventListener("input",e=>{FX.amount=e.target.value/100;syncFixUI();scheduleFix();});
$("fixAng").addEventListener("input",e=>{FX.angle=+e.target.value;if(FX.mode==="auto")FX.mode="motion";syncFixUI();scheduleFix();});
$("fixLen").addEventListener("input",e=>{FX.len=e.target.value/100*FIX_LEN_MAX;if(FX.mode==="auto")FX.mode=fixType()==="sharp"?"defocus":fixType();syncFixUI();scheduleFix();});
function fixOnNewImage(){baseData=null;FX.an=null;if(FX.on){runAnalysis();FX.mode="auto";scheduleFix();}syncFixUI();}
function fixReset(){FX.on=false;FX.mode="auto";FX.amount=1;baseData=null;syncFixUI();}
