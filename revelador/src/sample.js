function makeSample(){
  const c=document.createElement("canvas");c.width=1800;c.height=1200;const x=c.getContext("2d");
  let g=x.createLinearGradient(0,0,0,760);g.addColorStop(0,"#4f84bd");g.addColorStop(.55,"#9fc2df");g.addColorStop(1,"#ecd9bb");
  x.fillStyle=g;x.fillRect(0,0,1800,780);
  g=x.createRadialGradient(1260,600,10,1260,600,420);g.addColorStop(0,"rgba(255,248,225,1)");g.addColorStop(.12,"rgba(255,236,190,.9)");g.addColorStop(1,"rgba(255,220,170,0)");
  x.fillStyle=g;x.fillRect(0,0,1800,780);
  x.fillStyle="rgba(255,255,255,.55)";
  [[300,190,160],[420,170,120],[540,200,140],[1350,250,130],[1450,230,100],[980,140,90]].forEach(([a,b,r])=>{x.beginPath();x.ellipse(a,b,r,r*.38,0,0,7);x.fill();});
  const ridge=(base,amp,f1,f2,col)=>{x.beginPath();x.moveTo(0,1200);for(let i=0;i<=1800;i+=6){x.lineTo(i,base-amp*(.55*Math.sin(i*f1)+.3*Math.sin(i*f2+1.7)+.15*Math.sin(i*.031)));}x.lineTo(1800,1200);x.closePath();x.fillStyle=col;x.fill();};
  ridge(640,110,.0042,.011,"#7d8fa6");
  ridge(720,90,.0061,.017,"#4f6b58");
  g=x.createLinearGradient(0,780,0,1200);g.addColorStop(0,"#7fa3c2");g.addColorStop(1,"#2b4a61");x.fillStyle=g;x.fillRect(0,780,1800,420);
  x.globalAlpha=.35;x.fillStyle="#fff1d0";for(let i=0;i<40;i++){x.fillRect(1180+Math.sin(i*3.1)*60,800+i*9,160-i*2.5,2);}x.globalAlpha=1;
  x.beginPath();x.moveTo(0,860);x.bezierCurveTo(300,820,560,860,760,960);x.bezierCurveTo(820,1040,700,1200,0,1200);x.closePath();
  g=x.createLinearGradient(0,840,0,1200);g.addColorStop(0,"#5f8f3c");g.addColorStop(1,"#23401d");x.fillStyle=g;x.fill();
  x.fillStyle="#a8452f";x.fillRect(360,835,110,70);x.fillStyle="#3d2a22";x.beginPath();x.moveTo(345,840);x.lineTo(415,795);x.lineTo(485,840);x.fill();
  x.fillStyle="#f1d27a";x.fillRect(395,860,22,20);
  x.fillStyle="#c98f6e";x.beginPath();x.arc(600,930,20,0,7);x.fill();x.fillStyle="#2f3b56";x.fillRect(582,950,36,70);
  return c;
}
