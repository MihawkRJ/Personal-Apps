/* ---------------- Predefinições ---------------- */
// Parâmetros são deltas a partir do neutro. st/ht = tingimento de sombras/realces (RGB).
const SCENES = [
  {id:"none", nm:"Nenhuma", p:{}},
  {id:"golden", nm:"Golden hour", p:{temp:.45,tint:.05,exposure:.1,contrast:.1,highlights:-.25,shadows:.15,sat:.08,vib:.22,ht:[.05,.02,-.05],st:[.02,0,-.02]}},
  {id:"sunset", nm:"Fim de tarde", p:{temp:.6,tint:.14,exposure:-.1,contrast:.2,sat:.18,highlights:-.2,ht:[.06,0,-.03],st:[.01,-.01,.045],vignette:.25}},
  {id:"blue", nm:"Hora azul", p:{temp:-.4,tint:.05,contrast:.1,shadows:.12,sat:.1,st:[-.02,0,.05],ht:[.03,.01,-.02]}},
  {id:"night", nm:"Noturna", p:{exposure:.45,shadows:.55,highlights:-.45,whites:.55,contrast:.05,temp:-.08,vib:.18,st:[-.01,0,.025]}},
  {id:"sunny", nm:"Dia ensolarado", p:{exposure:.05,contrast:.15,vib:.25,temp:.08,highlights:-.2,shadows:.05}},
  {id:"cloudy", nm:"Nublado", p:{exposure:.15,contrast:.22,temp:.2,vib:.3,shadows:.1,highlights:-.1}},
  {id:"indoor", nm:"Luz interna", p:{temp:-.35,tint:-.04,exposure:.1,shadows:.15,highlights:-.1,vib:.05}},
  {id:"hdr", nm:"Contraluz", p:{shadows:.6,highlights:-.55,whites:.3,contrast:.08,vib:.12}},
];
const GRADES = [
  {id:"none", nm:"Nenhum", p:{}},
  {id:"teal", nm:"Teal & Orange", p:{st:[-.05,.02,.055],ht:[.06,.02,-.05],contrast:.15,sat:.03,vib:.1}},
  {id:"muted", nm:"Cinema suave", p:{sat:-.22,contrast:-.05,blacks:.05,whites:.3,ht:[.02,.01,-.01],st:[-.01,.01,.02]}},
  {id:"vintage", nm:"Filme vintage", p:{blacks:.08,whites:.45,sat:-.15,temp:.15,st:[.015,.03,-.02],ht:[.04,.02,-.04],grain:.35,vignette:.3}},
  {id:"moody", nm:"Moody", p:{exposure:-.2,contrast:.22,sat:-.25,shadows:-.1,st:[-.02,.01,.03],vignette:.4,blacks:.03}},
  {id:"bleach", nm:"Bleach bypass", p:{sat:-.5,contrast:.4,highlights:-.1,whites:.2}},
  {id:"pastel", nm:"Pastel claro", p:{exposure:.15,contrast:-.22,blacks:.06,sat:-.1,ht:[.02,.005,.02]}},
  {id:"chrome", nm:"Cromo vibrante", p:{sat:.25,contrast:.25,temp:.05,st:[0,0,.03],ht:[.03,.01,-.02]}},
  {id:"bw", nm:"P&B clássico", p:{mono:1,contrast:.25,grain:.2}},
];
const CAMS = [
  {id:"none", nm:"Sem perfil", sb:"cor da foto", p:{}},
  {id:"arri", nm:"ARRI Alexa 35", sb:"LogC4 → Rec.709", p:{contrast:-.04,whites:.65,sat:-.06,ht:[.015,.005,-.012],st:[0,.004,.006],
     matrix:[[1.03,-.04,.01],[-.02,.98,.04],[0,-.05,1.05]]}},
  {id:"red", nm:"RED V-Raptor", sb:"IPP2 → Rec.709", p:{contrast:.15,whites:.35,sat:.08,st:[-.006,0,.012],
     matrix:[[1.05,-.04,-.01],[-.02,1.04,-.02],[0,-.03,1.03]]}},
  {id:"sony", nm:"Sony Venice 2", sb:"S-Cinetone", p:{contrast:.05,whites:.45,tint:.04,temp:.05,blacks:.01,
     matrix:[[1.03,-.02,-.01],[-.01,1.0,.01],[0,-.02,1.02]]}},
  {id:"canon", nm:"Canon C500 II", sb:"Cinema Gamut → 709", p:{temp:.1,sat:.1,contrast:.08,whites:.3,ht:[.01,.005,-.01],
     matrix:[[1.06,-.04,-.02],[-.01,1.01,0],[-.01,-.03,1.04]]}},
];
const FINE = [
  {k:"exposure", nm:"Exposição", min:-2, max:2, step:.05, fmt:v=>(v>0?"+":"")+v.toFixed(2)+" EV", scale:1},
  {k:"temp", nm:"Temperatura", min:-100, max:100, step:1, fmt:v=>(v>0?"+":"")+v+(v>0?" quente":v<0?" fria":""), scale:.01},
  {k:"tint", nm:"Matiz (verde ↔ magenta)", min:-100, max:100, step:1, fmt:v=>(v>0?"+":"")+v, scale:.01},
  {k:"contrast", nm:"Contraste", min:-100, max:100, step:1, fmt:v=>(v>0?"+":"")+v, scale:.01},
  {k:"highlights", nm:"Realces", min:-100, max:100, step:1, fmt:v=>(v>0?"+":"")+v, scale:.01},
  {k:"shadows", nm:"Sombras", min:-100, max:100, step:1, fmt:v=>(v>0?"+":"")+v, scale:.01},
  {k:"sat", nm:"Saturação", min:-100, max:100, step:1, fmt:v=>(v>0?"+":"")+v, scale:.01},
  {k:"vib", nm:"Vibração", min:-100, max:100, step:1, fmt:v=>(v>0?"+":"")+v, scale:.01},
  {k:"vignette", nm:"Vinheta", min:0, max:100, step:1, fmt:v=>v+"", scale:.01},
  {k:"grain", nm:"Granulação", min:0, max:100, step:1, fmt:v=>v+"", scale:.01},
];
