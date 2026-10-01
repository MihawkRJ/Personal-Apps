/* 5 variantes de "Golden hour" para o Revelador. Mesmo formato dos presets (deltas do neutro).
   Cada uma usa uma estratégia diferente, não só mais ou menos intensidade. */
const VARIANTS = [
  {id:"atual", nm:"Atual (v1.1)", ds:"Preset que já está no app, como referência.",
   p:{temp:.45,tint:.05,exposure:.1,contrast:.1,highlights:-.25,shadows:.15,sat:.08,vib:.22,ht:[.05,.02,-.05],st:[.02,0,-.02]}},
  {id:"v1", nm:"1 · Ouro nos realces", ds:"Dourado só nas partes claras; sombras ficam neutras (split-tone).",
   p:{temp:.18,tint:.02,exposure:.04,contrast:.1,highlights:-.3,shadows:.12,vib:.18,ht:[.11,.045,-.09],st:[-.005,0,.012],whites:.2}},
  {id:"v2", nm:"2 · Sol baixo cinema", ds:"Luz laranja com sombras frias: contraste de cor, estilo cinema.",
   p:{temp:.5,tint:.06,exposure:-.05,contrast:.2,highlights:-.32,shadows:.1,sat:.05,vib:.2,ht:[.08,.03,-.07],st:[-.03,0,.04],vignette:.18}},
  {id:"v3", nm:"3 · Névoa dourada", ds:"Sombras levantadas e brilho suave, como luz atravessando neblina.",
   p:{temp:.38,tint:.03,exposure:.08,contrast:-.08,highlights:-.18,shadows:.22,blacks:.05,vib:.14,sat:-.02,ht:[.06,.03,-.02],st:[.025,.012,-.01],whites:.25}},
  {id:"v4", nm:"4 · Natural fiel", ds:"Toque leve que preserva a foto; pensado para pele e céu naturais.",
   p:{temp:.24,tint:.02,exposure:.04,contrast:.06,highlights:-.15,shadows:.08,vib:.14,ht:[.03,.012,-.025]}},
  {id:"v5", nm:"5 · Âmbar intenso", ds:"Magic hour forte e saturado, para fotos de impacto.",
   p:{temp:.72,tint:.1,exposure:-.08,contrast:.24,highlights:-.28,shadows:.06,sat:.12,vib:.2,ht:[.09,.025,-.08],st:[.012,-.008,.02],vignette:.3,
      matrix:[[1.05,-.03,-.02],[0,1.01,-.01],[-.01,-.04,1.0]]}},
];
