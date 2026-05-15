#!/usr/bin/env python3
# C:/Users/usuario/ownCloud2/RankGA/scripts/racing_visualizer_template.py
# Provide CSS and JavaScript templates for the standalone racing visualizer.


def css() -> str:
    return """
:root{--ink:#17201a;--muted:#5e675f;--paper:#f4efe4;--panel:#fffaf0;--track:#d8d0bf;--line:#26352b;--accent:#d64f2a;--blue:#2f6f88}
*{box-sizing:border-box}body{margin:0;background:radial-gradient(circle at top left,#fff7e4,#e8dcc5 46%,#c9d3c3);color:var(--ink);font:16px/1.45 Georgia,serif}
main{display:grid;grid-template-columns:390px 1fr;gap:18px;min-height:100vh;padding:18px}
.panel{background:rgba(255,250,240,.9);border:1px solid rgba(23,32,26,.18);border-radius:18px;padding:18px;box-shadow:0 20px 50px rgba(36,42,31,.18)}
h1{font-size:28px;line-height:1.05;margin:0 0 12px}.warning{border-left:4px solid var(--accent);padding-left:10px;color:#59311f}.stats{display:grid;gap:6px;margin:14px 0}
.stat{display:flex;justify-content:space-between;border-bottom:1px dotted #b3aa98;padding:4px 0}.controls{display:grid;gap:10px;margin-top:16px}
button,select,input{font:inherit}button{border:0;border-radius:999px;background:var(--ink);color:white;padding:10px 14px;cursor:pointer}button:hover{background:#314437}
input[type=file]{max-width:210px;font-size:13px}label{display:flex;align-items:center;justify-content:space-between;gap:10px}.hint{margin:0;color:var(--muted);font-size:14px}.canvasWrap{background:#f8f1df;border-radius:22px;padding:10px;box-shadow:inset 0 0 0 1px rgba(23,32,26,.16),0 20px 60px rgba(36,42,31,.2)}
canvas{width:100%;height:auto;display:block;border-radius:16px;background:linear-gradient(145deg,#efe5cf,#d9dfd0);cursor:grab;touch-action:none}
canvas.panning{cursor:grabbing}
code{font-size:12px;word-break:break-all}@media(max-width:900px){main{grid-template-columns:1fr}}
"""


def javascript() -> str:
    return """
const canvas=document.getElementById('canvas');
const ctx=canvas.getContext('2d');
const select=document.getElementById('runSelect');
const play=document.getElementById('play');
const speed=document.getElementById('speed');
let playing=false,startWall=0,simTime=0,panning=false,lastPan={x:0,y:0};
let view={scale:1,offsetX:0,offsetY:0};
let trackBounds,anchorBounds,anchorViewBounds;

function setup(){
  bindControls();
  refreshBounds();
  populateRunSelect();
  fitBounds(trackBounds,false);
  renderStats();
  renderGeneWarning();
  draw(0);
}

function bindControls(){
  play.onclick=togglePlayback;
  document.getElementById('reset').onclick=resetPlayback;
  document.getElementById('fitTrack').onclick=()=>fitBounds(trackBounds);
  document.getElementById('fitAnchors').onclick=()=>fitBounds(anchorViewBounds);
  document.getElementById('populationFile').onchange=loadSelectedPopulationFile;
  select.onchange=()=>{simTime=0;draw(0)};
  document.getElementById('showAnchors').onchange=()=>draw(simTime);
  document.getElementById('showAnchorBox').onchange=()=>draw(simTime);
  document.getElementById('showTrails').onchange=()=>draw(simTime);
  canvas.addEventListener('wheel',handleWheel,{passive:false});
  canvas.addEventListener('pointerdown',startPan);
  canvas.addEventListener('pointermove',movePan);
  canvas.addEventListener('pointerup',stopPan);
  canvas.addEventListener('pointercancel',stopPan);
}

function populateRunSelect(){
  select.innerHTML='<option value="all">Todas</option>'+DATA.runs.map(r=>`<option value="${r.index}">${r.index}</option>`).join('');
}

function refreshBounds(){
  trackBounds=boundsOf(viewportPoints());
  anchorBounds=boundsOf(DATA.anchors,0);
  anchorViewBounds=boundsOf(DATA.anchors,.08);
}

function setFileStatus(message,isError=false){
  const status=document.getElementById('fileStatus');
  status.textContent=message;
  status.style.color=isError?'#8b2717':'var(--muted)';
}

function loadSelectedPopulationFile(event){
  const file=event.target.files&&event.target.files[0];
  if(!file)return;
  setFileStatus(`Leyendo ${file.name}...`);
  const reader=new FileReader();
  reader.onerror=()=>setFileStatus(
    `No se pudo leer ${file.name}. Si es el archivo activo de RankGA, carga una copia snapshot porque puede estar siendo reescrito.`,
    true);
  reader.onload=()=>setTimeout(()=>loadPopulationText(String(reader.result),file.name),0);
  reader.readAsText(file);
}

function loadPopulationText(text,sourceName){
  try{
    setFileStatus(`Procesando ${sourceName}...`);
    const row=readBestPopulationRow(text);
    const genes=parseGenes(row.genes);
    const timeLimit=row.extra.T;
    const goalDistance=DATA.goalDistance;
    DATA.source=sourceName;
    DATA.geneFormat='full-precision whitespace';
    DATA.logged=row.extra;
    DATA.timeLimit=timeLimit;
    DATA.anchors=buildAnchors(genes);
    DATA.runs=DATA.starts.map((start,index)=>simulateRun(genes,start,timeLimit,goalDistance,index));
    playing=false;
    play.textContent='Reproducir';
    simTime=0;
    populateRunSelect();
    refreshBounds();
    fitBounds(trackBounds,false);
    renderStats();
    renderGeneWarning();
    draw(0);
    setFileStatus(`Cargado ${sourceName}: fila rank ${row.rank}, fitness ${row.extra.fitness.toFixed(12)}.`);
  }catch(error){
    setFileStatus(`Archivo rechazado: ${error.message}`,true);
  }
}

function readBestPopulationRow(text){
  let best=null;
  for(const line of text.split(/\\r?\\n/)){
    if(!/^\\s*\\d+\\s*,/.test(line))continue;
    const parts=splitPopulationLine(line);
    const row={
      rank:parseInt(parts[0].trim(),10),
      mutationIntensity:parseFloat(parts[1].trim()),
      fitness:parseFloat(parts[2].trim()),
      extra:parseExtra(parts[3].trim()),
      genes:parts[4].trim()
    };
    if(!Number.isFinite(row.rank)||!Number.isFinite(row.fitness)){
      throw new Error('la fila de población contiene rank o fitness inválido');
    }
    if(best===null||row.fitness>best.fitness)best=row;
  }
  if(best===null)throw new Error('no se encontró ninguna fila de población válida');
  return best;
}

function splitPopulationLine(line){
  const indexes=[];
  for(let index=0;index<line.length&&indexes.length<4;index++){
    if(line[index]===',')indexes.push(index);
  }
  if(indexes.length<4)throw new Error('la línea CSV no tiene las 5 columnas esperadas');
  return [
    line.slice(0,indexes[0]),
    line.slice(indexes[0]+1,indexes[1]),
    line.slice(indexes[1]+1,indexes[2]),
    line.slice(indexes[2]+1,indexes[3]),
    line.slice(indexes[3]+1)
  ];
}

function parseExtra(extraText){
  const match=extraText.match(/distance=([0-9.Ee+-]+)\\s+offTrack=(true|false)\\s+goalReached=(true|false)\\s+avgSpeed=([0-9.Ee+-]+)\\s+T=([0-9.Ee+-]+)\\s+M=(\\d+)\\s+fitness=([0-9.Ee+-]+)\\s+safeRuns=(\\d+)\\s+avgDistance=([0-9.Ee+-]+)\\s+goalRuns=(\\d+)\\s+avgGoalTime=([0-9.Ee+-]+)/);
  if(!match)throw new Error('el extra string no tiene el formato vigente');
  return {
    distance:parseFloat(match[1]),offTrack:match[2]==='true',goalReached:match[3]==='true',
    avgSpeed:parseFloat(match[4]),T:parseFloat(match[5]),M:parseInt(match[6],10),
    fitness:parseFloat(match[7]),safeRuns:parseInt(match[8],10),
    avgDistance:parseFloat(match[9]),goalRuns:parseInt(match[10],10),
    avgGoalTime:parseFloat(match[11])
  };
}

function parseGenes(text){
  const tokens=text.trim().split(/\\s+/);
  const expected=DATA.simulation.countOfAnchors*DATA.simulation.genesPerAnchor;
  if(tokens.length!==expected)throw new Error(`se esperaban ${expected} genes separados por espacios, pero se leyeron ${tokens.length}`);
  const genes=tokens.map(Number);
  if(genes.some(value=>!Number.isFinite(value)))throw new Error('hay genes no numéricos');
  return genes;
}

function buildAnchors(genes){
  const anchors=[];
  for(let index=0;index<DATA.simulation.countOfAnchors;index++){
    const offset=index*DATA.simulation.genesPerAnchor;
    anchors.push({x:genes[offset],y:genes[offset+1],speed:genes[offset+2],direction:genes[offset+3]});
  }
  return anchors;
}

function simulateRun(genes,start,timeLimit,goalDistance,runIndex){
  let state={x:start.x,y:start.y,speed:start.speed,heading:wrapToPi(start.heading),time:0};
  let lastLapProgress=projectProgress(state.x,state.y),progress=0,offTrack=false;
  const points=[pointDict(state,0,true)];
  while(state.time<timeLimit){
    const policy=interpolatePolicy(genes,state);
    const action=adaptAction(policy,state);
    const step=stepBackend(state,action,lastLapProgress,progress);
    state=step.state;lastLapProgress=step.lastLapProgress;progress=step.progress;offTrack=step.offTrack;
    points.push(pointDict(state,progress,!offTrack));
    if(offTrack||progress>=goalDistance)break;
  }
  const reachedGoal=progress>=goalDistance&&!offTrack;
  const averageSpeed=state.time>0?(reachedGoal?goalDistance:progress)/state.time:0;
  return {index:runIndex,points,distance:progress,offTrack,reachedGoal,avgSpeed:averageSpeed,duration:state.time};
}

function stepBackend(state,action,lastLapProgress,progress){
  const sim=DATA.simulation;
  const acceleration=sim.throttleAcceleration*action.throttle-sim.brakeAcceleration*action.brake-sim.dragCoefficient*state.speed;
  const nextSpeed=clampJs(state.speed+acceleration*sim.timeStepSeconds,0,DATA.maxSpeed);
  const averageSpeed=.5*(state.speed+nextSpeed);
  const nextHeading=wrapToPi(state.heading+sim.steeringRateGain*action.steering*sim.timeStepSeconds);
  const nextX=state.x+averageSpeed*Math.cos(nextHeading)*sim.timeStepSeconds;
  const nextY=state.y+averageSpeed*Math.sin(nextHeading)*sim.timeStepSeconds;
  const nextState={x:nextX,y:nextY,speed:nextSpeed,heading:nextHeading,time:state.time+sim.timeStepSeconds};
  const nextLapProgress=projectProgress(nextX,nextY);
  const nextProgress=progress+wrapProgressDelta(nextLapProgress-lastLapProgress,DATA.lapLength);
  return {state:nextState,lastLapProgress:nextLapProgress,progress:nextProgress,offTrack:!isInsideTrack(nextX,nextY)};
}

function interpolatePolicy(genes,state){
  let weightedSpeed=0,weightedDirectionX=0,weightedDirectionY=0,totalWeight=0;
  const sim=DATA.simulation;
  for(let anchorIndex=0;anchorIndex<sim.countOfAnchors;anchorIndex++){
    const offset=anchorIndex*sim.genesPerAnchor;
    const d=Math.hypot(state.x-genes[offset],state.y-genes[offset+1]);
    const weight=1/Math.pow(d+sim.policyEpsilon,sim.inverseDistancePower);
    weightedSpeed+=weight*genes[offset+2];
    weightedDirectionX+=weight*Math.cos(genes[offset+3]);
    weightedDirectionY+=weight*Math.sin(genes[offset+3]);
    totalWeight+=weight;
  }
  if(totalWeight<=0)throw new Error('la interpolación produjo peso total no positivo');
  return {speedTarget:weightedSpeed/totalWeight,directionTarget:Math.atan2(weightedDirectionY,weightedDirectionX)};
}

function adaptAction(policy,state){
  const speedError=policy.speedTarget-state.speed;
  return {
    steering:clampJs(wrapToPi(policy.directionTarget-state.heading),-1,1),
    throttle:speedError>0?clampJs(.2*speedError,0,1):0,
    brake:speedError<0?clampJs(-.2*speedError,0,1):0
  };
}

function projectProgress(x,y){
  return projectTrack(x,y).progress;
}

function isInsideTrack(x,y){
  return projectTrack(x,y).distance<=DATA.simulation.trackHalfWidth;
}

function projectTrack(x,y){
  const sim=DATA.simulation,r=sim.trackRadius,h=sim.trackHalfStraight;
  const straightLength=2*h,turnLength=Math.PI*r;
  const candidates=[];
  const topX=clampJs(x,-h,h);
  candidates.push({progress:topX+h,distance:Math.hypot(x-topX,y-r)});
  const rightAngle=clampJs(Math.atan2(y,x-h),-Math.PI/2,Math.PI/2);
  candidates.push({progress:straightLength+(Math.PI/2-rightAngle)*r,distance:Math.hypot(x-(h+r*Math.cos(rightAngle)),y-r*Math.sin(rightAngle))});
  const bottomX=clampJs(x,-h,h);
  candidates.push({progress:straightLength+turnLength+(h-bottomX),distance:Math.hypot(x-bottomX,y+r)});
  const leftAngle=clampJs(Math.atan2(y,-(x+h)),-Math.PI/2,Math.PI/2);
  candidates.push({progress:straightLength*2+turnLength+(leftAngle+Math.PI/2)*r,distance:Math.hypot(x-(-h-r*Math.cos(leftAngle)),y-r*Math.sin(leftAngle))});
  return candidates.reduce((best,item)=>item.distance<best.distance?item:best,candidates[0]);
}

function pointDict(state,progress,inside){
  return {x:state.x,y:state.y,speed:state.speed,heading:state.heading,time:state.time,progress,inside};
}

function renderGeneWarning(){
  document.getElementById('geneWarning').textContent=`Genes leídos en formato vigente: ${DATA.geneFormat}.`;
}

function renderStats(){
  const best=DATA.logged,visible=countOfVisibleAnchors(),hidden=DATA.anchors.length-visible;
  const boxWidth=anchorBounds.maxX-anchorBounds.minX,boxHeight=anchorBounds.maxY-anchorBounds.minY;
  const rows=[
    ['Fitness registrado',best.fitness.toFixed(6)],['Distancia registrada',best.distance.toFixed(6)+' m'],
    ['Meta',best.goalReached?'sí':'no'],['Salida de pista',best.offTrack?'sí':'no'],
    ['Velocidad media',best.avgSpeed.toFixed(6)+' m/s'],['T',best.T.toFixed(6)+' s'],['M',best.M],
    ['Vuelta',DATA.lapLength.toFixed(6)+' m'],['Corridas sin salida',`${best.safeRuns}/${best.M}`],
    ['Corridas meta',`${best.goalRuns}/${best.M}`],['Tiempo medio meta',best.avgGoalTime.toFixed(6)+' s'],
    ['Caja de anclas',`${boxWidth.toFixed(2)} × ${boxHeight.toFixed(2)} m`],
    ['Anclas visibles',`${visible}/${DATA.anchors.length}`],['Zoom',`${view.scale.toFixed(2)} px/m`]
  ];
  document.getElementById('stats').innerHTML=rows.map(r=>`<div class="stat"><span>${r[0]}</span><strong>${r[1]}</strong></div>`).join('');
  document.getElementById('viewportWarning').textContent=hidden>0?`${hidden} anclas quedan fuera de la vista actual. Usa zoom/pan o "Vista anclas" para inspeccionarlas.`:'';
}

function clampJs(value,min,max){return Math.max(min,Math.min(max,value))}
function finitePoint(p){return Number.isFinite(p.x)&&Number.isFinite(p.y)}
function viewportPoints(){return[...DATA.track.left,...DATA.track.right,...DATA.track.center,...DATA.starts,...DATA.runs.flatMap(r=>r.points)].filter(finitePoint)}
function boundsOf(points,padRatio=.08){
  const pts=points.filter(finitePoint),xs=pts.map(p=>p.x),ys=pts.map(p=>p.y);
  const minX=Math.min(...xs),maxX=Math.max(...xs),minY=Math.min(...ys),maxY=Math.max(...ys);
  const pad=Math.max(maxX-minX,maxY-minY,1)*padRatio;
  return {minX:minX-pad,maxX:maxX+pad,minY:minY-pad,maxY:maxY+pad};
}
function fitBounds(box,redraw=true){
  const width=Math.max(box.maxX-box.minX,1),height=Math.max(box.maxY-box.minY,1),cx=(box.minX+box.maxX)/2,cy=(box.minY+box.maxY)/2;
  view.scale=Math.min(canvas.width/width,canvas.height/height);
  view.offsetX=canvas.width/2-cx*view.scale;
  view.offsetY=canvas.height/2+cy*view.scale;
  if(redraw){renderStats();draw(simTime)}
}
function sx(x){return view.offsetX+x*view.scale}
function sy(y){return view.offsetY-y*view.scale}
function screenPoint(event){const rect=canvas.getBoundingClientRect();return{x:(event.clientX-rect.left)*canvas.width/rect.width,y:(event.clientY-rect.top)*canvas.height/rect.height}}
function worldAt(p){return{x:(p.x-view.offsetX)/view.scale,y:(view.offsetY-p.y)/view.scale}}
function inScreen(p){if(!finitePoint(p))return false;const x=sx(p.x),y=sy(p.y);return x>=-40&&x<=canvas.width+40&&y>=-40&&y<=canvas.height+40}
function visibleAnchors(){return DATA.anchors.filter(inScreen)}
function countOfVisibleAnchors(){return visibleAnchors().length}
function path(points,close=false){ctx.beginPath();points.forEach((p,i)=>{if(i===0)ctx.moveTo(sx(p.x),sy(p.y));else ctx.lineTo(sx(p.x),sy(p.y))});if(close)ctx.closePath()}
function pointAt(run,t){
  const pts=run.points;if(t<=0)return pts[0];
  for(let i=1;i<pts.length;i++){if(pts[i].time>=t){const a=pts[i-1],b=pts[i],u=(t-a.time)/(b.time-a.time||1);return{x:a.x+(b.x-a.x)*u,y:a.y+(b.y-a.y)*u,heading:a.heading+(b.heading-a.heading)*u,speed:a.speed+(b.speed-a.speed)*u,time:t,progress:a.progress+(b.progress-a.progress)*u,inside:b.inside}}}
  return pts[pts.length-1];
}
function selectedRuns(){return select.value==='all'?DATA.runs:DATA.runs.filter(r=>String(r.index)===select.value)}
function anchorTargetLength(a){const speedTarget=Number.isFinite(a.speed)?Math.max(0,a.speed):0;return 12+Math.min(speedTarget,DATA.maxSpeed)/DATA.maxSpeed*112}
function drawAnchorTarget(a){const x=sx(a.x),y=sy(a.y),length=anchorTargetLength(a);ctx.beginPath();ctx.moveTo(x,y);ctx.lineTo(x+Math.cos(a.direction)*length,y-Math.sin(a.direction)*length);ctx.stroke()}
function drawAnchorBox(){const b=anchorBounds;ctx.save();ctx.setLineDash([10,6]);ctx.strokeStyle='rgba(214,79,42,.95)';ctx.lineWidth=2;ctx.strokeRect(sx(b.minX),sy(b.maxY),(b.maxX-b.minX)*view.scale,(b.maxY-b.minY)*view.scale);ctx.setLineDash([]);ctx.fillStyle='rgba(214,79,42,.95)';ctx.font='13px Georgia';ctx.fillText('bounding box anclas',sx(b.minX)+6,sy(b.maxY)-8);ctx.restore()}
function drawAnchors(){const anchors=visibleAnchors();ctx.save();ctx.strokeStyle='rgba(122,82,18,.76)';ctx.lineWidth=1.8;ctx.lineCap='round';anchors.forEach(drawAnchorTarget);ctx.restore();anchors.forEach(a=>{ctx.beginPath();ctx.arc(sx(a.x),sy(a.y),3,0,Math.PI*2);ctx.fillStyle=a.speed>=0?'rgba(47,111,136,.55)':'rgba(214,79,42,.55)';ctx.fill()})}
function draw(t){
  ctx.clearRect(0,0,canvas.width,canvas.height);
  const ribbon=[...DATA.track.left,...DATA.track.right.slice().reverse()];
  path(ribbon,true);ctx.fillStyle='rgba(186,178,160,.92)';ctx.fill();
  path(DATA.track.left,true);ctx.strokeStyle='#26352b';ctx.lineWidth=2;ctx.stroke();
  path(DATA.track.right,true);ctx.stroke();path(DATA.track.center,true);ctx.setLineDash([7,7]);ctx.strokeStyle='rgba(38,53,43,.45)';ctx.lineWidth=1.3;ctx.stroke();ctx.setLineDash([]);
  if(document.getElementById('showAnchorBox').checked)drawAnchorBox();
  if(document.getElementById('showAnchors').checked)drawAnchors();
  selectedRuns().forEach(r=>{const color=['#d64f2a','#2f6f88','#57783f','#8a5a2b','#6e4b8b','#1f8a70','#a83a3a'][r.index%7];if(document.getElementById('showTrails').checked){const trail=r.points.filter(p=>p.time<=t);path(trail);ctx.strokeStyle=color;ctx.globalAlpha=.72;ctx.lineWidth=3;ctx.stroke();ctx.globalAlpha=1}const p=pointAt(r,t);drawCar(p,color);drawLabel(p,`run ${r.index}`)});
  requestAnimationFrameMaybe();
}
function drawCar(p,color){ctx.save();ctx.translate(sx(p.x),sy(p.y));ctx.rotate(-p.heading);ctx.fillStyle=color;ctx.strokeStyle='#111';ctx.lineWidth=1.5;ctx.beginPath();ctx.moveTo(12,0);ctx.lineTo(-8,-5);ctx.lineTo(-6,0);ctx.lineTo(-8,5);ctx.closePath();ctx.fill();ctx.stroke();ctx.restore()}
function drawLabel(p,text){ctx.fillStyle='#17201a';ctx.font='14px Georgia';ctx.fillText(`${text}  ${p.progress.toFixed(1)} m`,sx(p.x)+10,sy(p.y)-10)}
function togglePlayback(){playing=!playing;play.textContent=playing?'Pausar':'Reproducir';if(playing){startWall=performance.now()-simTime*1000/parseFloat(speed.value);requestAnimationFrame(tick)}}
function resetPlayback(){playing=false;simTime=0;play.textContent='Reproducir';draw(0)}
function tick(){if(!playing)return;const rate=parseFloat(speed.value),maxT=Math.max(...selectedRuns().map(r=>r.duration));simTime=(performance.now()-startWall)/1000*rate;if(simTime>maxT){playing=false;play.textContent='Reproducir';simTime=maxT}draw(simTime)}
function requestAnimationFrameMaybe(){if(playing)requestAnimationFrame(tick)}
function handleWheel(event){event.preventDefault();const p=screenPoint(event),before=worldAt(p),factor=Math.exp(-event.deltaY*.001);view.scale=clampJs(view.scale*factor,.05,80);view.offsetX=p.x-before.x*view.scale;view.offsetY=p.y+before.y*view.scale;renderStats();draw(simTime)}
function startPan(event){panning=true;lastPan=screenPoint(event);canvas.setPointerCapture(event.pointerId);canvas.classList.add('panning')}
function movePan(event){if(!panning)return;const p=screenPoint(event);view.offsetX+=p.x-lastPan.x;view.offsetY+=p.y-lastPan.y;lastPan=p;renderStats();draw(simTime)}
function stopPan(event){panning=false;canvas.classList.remove('panning');if(canvas.hasPointerCapture&&canvas.hasPointerCapture(event.pointerId))canvas.releasePointerCapture(event.pointerId)}
function wrapToPi(angle){let wrapped=angle;while(wrapped<=-Math.PI)wrapped+=2*Math.PI;while(wrapped>Math.PI)wrapped-=2*Math.PI;return wrapped}
function wrapProgressDelta(delta,lapLength){if(delta>lapLength/2)return delta-lapLength;if(delta<-lapLength/2)return delta+lapLength;return delta}
setup();
"""
