(function(){
"use strict";
const $=id=>document.getElementById(id),canvas=$("game"),loading=$("loading"),toast=$("toast");
if(!window.THREE||!THREE.GLTFLoader){loading.textContent="3D-движок не загрузился.";return;}
const T=THREE,CHUNK=36,SPEED=8.1,GRAVITY=19,MAX_DPR=1.3;
let renderer,scene,camera,clock,player,model,parts={},velocityY=0,onGround=true,paused=false,ready=false;
let yaw=0,coins=0,dist=0,moveX=0,moveY=0,stickId=null,lookId=null,lastLookX=0,anim=0,toastTimer=0;
let chunks=new Map(),coinMeshes=[],obstacles=[],mapModels=[],worldThemeIndex=0,worldName='Зелёная долина';
const themes=[
 {ground:0x6c9954,path:0x91b86a,sky:0x8bc7e7,prop:0x477341},
 {ground:0xc2a15c,path:0xe0c47a,sky:0xf0bd78,prop:0x927041},
 {ground:0xb8d1d8,path:0xe9f5f5,sky:0x9ebfd7,prop:0x718c99},
 {ground:0x4b505e,path:0x737b8b,sky:0x303444,prop:0x555c70},
 {ground:0x938778,path:0xc9b99a,sky:0x91b7cf,prop:0x66574b}
];
function say(s){toast.textContent=s;toast.style.opacity="1";clearTimeout(toastTimer);toastTimer=setTimeout(()=>toast.style.opacity="0",1800);}
function mat(c){return new T.MeshStandardMaterial({color:c,roughness:.92});}
function rngFor(seed){let n=(seed*1664525+1013904223)>>>0;return()=>{n=(n*1664525+1013904223)>>>0;return n/4294967296;};}
function addBear(){
 const root=new T.Group(),fur=mat(0x95603e),cream=mat(0xe7c6a0),black=mat(0x171717);
 const body=new T.Mesh(new T.SphereGeometry(.48,16,12),fur);body.position.y=.83;root.add(body);
 const head=new T.Mesh(new T.SphereGeometry(.38,16,12),fur);head.position.set(0,1.42,-.06);root.add(head);
 [-.27,.27].forEach(x=>{const e=new T.Mesh(new T.SphereGeometry(.15,10,8),fur);e.position.set(x,1.68,-.05);root.add(e);});
 const muzzle=new T.Mesh(new T.SphereGeometry(.18,12,8),cream);muzzle.scale.set(1.1,.7,.8);muzzle.position.set(0,1.32,-.36);root.add(muzzle);
 const nose=new T.Mesh(new T.SphereGeometry(.07,8,6),black);nose.position.set(0,1.36,-.51);root.add(nose);
 [-.13,.13].forEach(x=>{const e=new T.Mesh(new T.SphereGeometry(.035,8,6),black);e.position.set(x,1.48,-.39);root.add(e);});
 return root;
}
function replaceBear(gltf){
 while(player.children.length)player.remove(player.children[0]);
 model=gltf.scene;
 model.rotation.y=Math.PI;
 model.updateMatrixWorld(true);
 const before=new T.Box3().setFromObject(model),size=before.getSize(new T.Vector3());
 const maxHeight=Math.max(.01,size.y);
 model.scale.setScalar(1.55/maxHeight);
 model.updateMatrixWorld(true);
 const after=new T.Box3().setFromObject(model);
 model.position.y-=after.min.y;
 model.traverse(o=>{if(o.isMesh){o.frustumCulled=true;o.castShadow=false;o.receiveShadow=true;}});
 player.add(model);parts={};
 ["arm_l_4","arm_r_10","leg_l_18","leg_r_22"].forEach(n=>{const o=model.getObjectByName(n);if(o)parts[n]=o;});
 say("Персонаж подогнан под рост 1,55 м");
}
function addTree(g,x,z,theme,s,colliders){
 const trunk=new T.Mesh(new T.CylinderGeometry(.15*s,.22*s,1.25*s,7),mat(0x6b4a31));trunk.position.set(x,.6*s,z);g.add(trunk);
 const col=theme===2?0xe4f1f3:theme===1?0x9b854a:theme===3?0x555c70:theme===4?0x736052:0x477a42;
 const crown=new T.Mesh(new T.ConeGeometry(.9*s,1.8*s,7),mat(col));crown.position.set(x,1.7*s,z);g.add(crown);
 colliders.push({x,z,rx:.30*s,rz:.30*s,height:1.45*s});
}
function addCoin(g,x,z,list){
 const m=new T.Mesh(new T.TorusGeometry(.3,.09,8,16),new T.MeshStandardMaterial({color:0xffd34f,metalness:.65,roughness:.28,emissive:0x442800}));
 m.position.set(x,1.18,z);m.rotation.y=Math.PI/2;g.add(m);list.push({mesh:m,taken:false});
}
function addRock(g,x,z,theme,s,colliders){
 const m=new T.Mesh(new T.DodecahedronGeometry(.7*s,0),mat([0x75816b,0x9b8054,0x9daeb7,0x6b7181,0x75695e][theme]));
 m.position.set(x,.35*s,z);m.rotation.z=.18;g.add(m);
 colliders.push({x,z,rx:.62*s,rz:.62*s,height:1.1*s});
}
function addCottage(g,x,z,theme,colliders,r){
 const width=4.5+r()*2.5,depth=4+r()*2.3,height=2.2+r()*.7;
 const wallColor=theme===2?0xcbd7df:theme===1?0xb78b55:theme===3?0x555765:0xc4a27d;
 const body=new T.Mesh(new T.BoxGeometry(width,height,depth),mat(wallColor));
 body.position.set(x,height/2,z);g.add(body);
 const roof=new T.Mesh(new T.ConeGeometry(width*.78,1.35,4),mat(theme===2?0x667b91:theme===3?0x393f50:0x884d35));
 roof.position.set(x,height+.62,z);roof.rotation.y=Math.PI/4;g.add(roof);
 const door=new T.Mesh(new T.BoxGeometry(.85,1.5,.12),mat(0x503529));
 door.position.set(x,height*.35,z-depth/2-.065);g.add(door);
 colliders.push({x,z,rx:width*.53,rz:depth*.53,height:height+1.3});
}
function addBigHouse(g,x,z,colliders){
 const body=new T.Mesh(new T.BoxGeometry(11,4.1,9),mat(0xc5ad8b));
 body.position.set(x,2.05,z);g.add(body);
 const roof=new T.Mesh(new T.ConeGeometry(7.9,3.1,4),mat(0x5d3730));
 roof.position.set(x,5.55,z);roof.rotation.y=Math.PI/4;g.add(roof);
 const door=new T.Mesh(new T.BoxGeometry(1.35,2.35,.16),mat(0x51382d));
 door.position.set(x,1.18,z-4.58);g.add(door);
 for(const wx of [-3.5,3.5]){
   const window=new T.Mesh(new T.BoxGeometry(1.25,1.1,.13),mat(0x8ec9dc));
   window.position.set(x+wx,2.45,z-4.6);g.add(window);
 }
 colliders.push({x,z,rx:5.7,rz:4.8,height:7.2});
}
function attachMap(entry){
 if(!mapModels.length||entry.mapGroup)return;
 const source=mapModels[Math.abs(entry.cx*31+entry.cz*17)%mapModels.length];
 const group=new T.Group(),clone=source.clone(true);
 clone.updateMatrixWorld(true);
 const original=new T.Box3().setFromObject(clone),size=original.getSize(new T.Vector3());
 const largest=Math.max(size.x,size.z,1);
 const scale=Math.min(1,24/largest);
 clone.scale.setScalar(scale);clone.updateMatrixWorld(true);
 const box=new T.Box3().setFromObject(clone);
 clone.position.x-=(box.min.x+box.max.x)/2;
 clone.position.z-=(box.min.z+box.max.z)/2;
 clone.position.y-=box.min.y;
 group.add(clone);entry.group.add(group);entry.mapGroup=group;
 clone.traverse(o=>{
   if(!o.isMesh||!o.geometry)return;
   const bb=new T.Box3().setFromObject(o),sz=bb.getSize(new T.Vector3()),c=bb.getCenter(new T.Vector3());
   if(sz.y<.45||sz.y>10||sz.x>24||sz.z>24)return;
   if(sz.x>21&&sz.z>21)return;
   entry.colliders.push({x:c.x-entry.group.position.x,z:c.z-entry.group.position.z,rx:Math.max(.35,sz.x*.45),rz:Math.max(.35,sz.z*.45),height:sz.y});
 });
}
function refreshMaps(){
 chunks.forEach(entry=>{if(entry.mapGroup)entry.group.remove(entry.mapGroup);entry.mapGroup=null;entry.colliders=entry.baseColliders.slice();attachMap(entry);});
}
function biomeName(i){
 return ["Зелёная долина","Песчаная бухта","Снежная долина","Черепашья деревня","Большой дом"][i]||"Неизведанная зона";
}
function makeChunk(cx,cz){
 const seed=((cx*73856093)^(cz*19349663)^0x5f3759df)>>>0;
 const r=rngFor(seed),themeIndex=Math.floor(r()*themes.length),th=themes[themeIndex],g=new T.Group();
 g.position.set(cx*CHUNK,0,cz*CHUNK);scene.add(g);
 const base=new T.Mesh(new T.BoxGeometry(CHUNK+.25,.8,CHUNK+.25),mat(th.ground));
 base.position.set(0,-.43,0);g.add(base);
 const pathMat=mat(th.path);
 const northSouth=new T.Mesh(new T.BoxGeometry(5.6,.035,CHUNK+.15),pathMat);northSouth.position.y=.005;g.add(northSouth);
 const eastWest=new T.Mesh(new T.BoxGeometry(CHUNK+.15,.035,5.6),pathMat);eastWest.position.y=.006;g.add(eastWest);
 const localCoins=[],localObs=[],colliders=[];
 // Keep the outside rim unobstructed so every generated zone joins its neighbours.
 for(let i=0;i<7;i++){
   const angle=r()*Math.PI*2,rad=10+r()*5,x=Math.cos(angle)*rad,z=Math.sin(angle)*rad;
   if(r()>.27)addTree(g,x,z,themeIndex,.72+r()*.48,colliders);
   if(r()>.48)addRock(g,x*.76,z*.76,themeIndex,.65+r()*.55,colliders);
 }
 if(themeIndex===0||themeIndex===1||themeIndex===3){
   const count=themeIndex===3?4:2;
   for(let i=0;i<count;i++){
     const x=(r()>.5?1:-1)*(7+r()*6),z=(r()>.5?1:-1)*(7+r()*6);
     addCottage(g,x,z,themeIndex,colliders,r);
   }
 }
 if(themeIndex===4) addBigHouse(g,(r()>.5?1:-1)*5,(r()>.5?1:-1)*4,colliders);
 if(themeIndex===2){
   for(let i=0;i<5;i++){
     const x=(r()>.5?1:-1)*(7+r()*6),z=(r()>.5?1:-1)*(6+r()*7);
     addTree(g,x,z,themeIndex,1+r()*.7,colliders);
   }
 }
 const lanes=[-7,-2,3,8];
 for(let i=0;i<5;i++){
   const x=lanes[Math.floor(r()*lanes.length)],z=lanes[Math.floor(r()*lanes.length)];
   if(r()>.35)addCoin(g,x,z,localCoins);
   if(r()>.78){
     const rock=new T.Mesh(new T.DodecahedronGeometry(.7,0),mat(th.prop));
     rock.position.set(x,.45,z);g.add(rock);localObs.push({mesh:rock,hit:false});
     colliders.push({x,z,rx:.72,rz:.72,height:1.2});
   }
 }
 const entry={cx,cz,group:g,coins:localCoins,obstacles:localObs,colliders,baseColliders:colliders.slice(),mapGroup:null,themeIndex};
 chunks.set(cx+","+cz,entry);
 coinMeshes.push(...localCoins);obstacles.push(...localObs);attachMap(entry);
}
function updateChunks(){
 const cx=Math.floor(player.position.x/CHUNK),cz=Math.floor(player.position.z/CHUNK);
 const radius=2;
 for(let x=cx-radius;x<=cx+radius;x++)for(let z=cz-radius;z<=cz+radius;z++){
   const key=x+","+z;if(!chunks.has(key))makeChunk(x,z);
 }
 for(const [key,e] of Array.from(chunks.entries())){
   if(Math.abs(e.cx-cx)>radius+1||Math.abs(e.cz-cz)>radius+1){
     scene.remove(e.group);
     e.coins.forEach(c=>c.mesh.visible=false);
     e.obstacles.forEach(o=>o.mesh.visible=false);
     chunks.delete(key);
   }
 }
 coinMeshes=coinMeshes.filter(c=>c.mesh.parent&&c.mesh.visible);
 obstacles=obstacles.filter(o=>o.mesh.parent&&o.mesh.visible);
 const current=chunks.get(cx+","+cz);
 if(current){
   worldThemeIndex=current.themeIndex;worldName=biomeName(current.themeIndex);
   scene.background.setHex(themes[worldThemeIndex].sky);scene.fog.color.setHex(themes[worldThemeIndex].sky);
   if($("hint"))$("hint").textContent="Локация: "+worldName+" · исследуй тропы к соседним зонам";
 }
}
function collidesAt(x,z){
 for(const e of chunks.values()){
   const ox=e.group.position.x,oz=e.group.position.z;
   for(const c of e.colliders){
     if(player.position.y>Math.max(.65,c.height-.15))continue;
     const dx=x-(ox+c.x),dz=z-(oz+c.z);
     if(Math.abs(dx)<c.rx+.43&&Math.abs(dz)<c.rz+.43)return true;
   }
 }
 return false;
}
function collectAndCollide(){
 const v=new T.Vector3();
 for(const c of coinMeshes){
   if(c.taken||!c.mesh.visible)continue;
   c.mesh.getWorldPosition(v);
   const dx=v.x-player.position.x,dz=v.z-player.position.z,dy=v.y-player.position.y-.9;
   if(dx*dx+dz*dz<1.5&&Math.abs(dy)<1.6){
     c.taken=true;c.mesh.visible=false;coins++;$("coins").textContent=coins;
   }
 }
}
function jump(){if(!ready||paused||!onGround)return;velocityY=8.2;onGround=false;}
function update(dt){
 let x=moveX,y=moveY;
 if(Math.abs(x)>1||Math.abs(y)>1){const l=Math.hypot(x,y);x/=l;y/=l;}
 const nextX=player.position.x+x*SPEED*dt;
 const nextZ=player.position.z+y*SPEED*dt;
 // Resolve X and Z independently: walls and buildings block movement, while open paths stay connected.
 if(!collidesAt(nextX,player.position.z))player.position.x=nextX;
 if(!collidesAt(player.position.x,nextZ))player.position.z=nextZ;
 if(!onGround){
   velocityY-=GRAVITY*dt;player.position.y+=velocityY*dt;
   if(player.position.y<=0){player.position.y=0;velocityY=0;onGround=true;}
 }
 // Ground is present beneath every generated neighbour chunk; this guard prevents falling through unloaded seams.
 if(player.position.y<0){player.position.y=0;velocityY=0;onGround=true;}
 const walking=Math.abs(x)+Math.abs(y)>.12;
 if(model){
   const s=Math.sin(anim*10)*.28;
   if(parts.arm_l_4)parts.arm_l_4.rotation.x=walking?s:0;
   if(parts.arm_r_10)parts.arm_r_10.rotation.x=walking?-s:0;
   if(parts.leg_l_18)parts.leg_l_18.rotation.x=walking?-s:0;
   if(parts.leg_r_22)parts.leg_r_22.rotation.x=walking?s:0;
   if(Math.abs(x)>.08){const wanted=x>0?-.12:.12;model.rotation.z+=(wanted-model.rotation.z)*Math.min(1,dt*4);}
   else model.rotation.z+=(0-model.rotation.z)*Math.min(1,dt*4);
 }
 collectAndCollide();
 dist=Math.floor(Math.hypot(player.position.x,player.position.z)*1.5);
 $("distance").textContent=dist;
}
function cameraUpdate(dt){
 const distance=5.7,height=2.55;
 const target=new T.Vector3(
   player.position.x+Math.sin(yaw)*distance,
   player.position.y+height,
   player.position.z+Math.cos(yaw)*distance
 );
 camera.position.lerp(target,Math.min(1,dt*5));
 camera.lookAt(player.position.x,player.position.y+1.05,player.position.z);
}
function resize(){if(!renderer)return;const w=innerWidth,h=innerHeight;camera.aspect=w/h;camera.updateProjectionMatrix();renderer.setPixelRatio(Math.min(devicePixelRatio||1,MAX_DPR));renderer.setSize(w,h,false);}
function setup(){
 renderer=new T.WebGLRenderer({canvas,antialias:false,alpha:false,powerPreference:"high-performance"});
 renderer.setPixelRatio(Math.min(devicePixelRatio||1,MAX_DPR));renderer.setSize(innerWidth,innerHeight,false);renderer.outputEncoding=T.sRGBEncoding;renderer.shadowMap.enabled=false;
 scene=new T.Scene();scene.background=new T.Color(themes[0].sky);scene.fog=new T.Fog(themes[0].sky,35,145);
 camera=new T.PerspectiveCamera(62,innerWidth/innerHeight,.1,280);camera.position.set(0,5,9);
 scene.add(new T.HemisphereLight(0xe9f7ff,0x68744d,1.5));
 const sun=new T.DirectionalLight(0xfff0d6,1.15);sun.position.set(-8,18,12);scene.add(sun);
 const fill=new T.DirectionalLight(0xb4d7ff,.4);fill.position.set(9,7,-12);scene.add(fill);
 clock=new T.Clock();player=new T.Group();player.position.set(0,0,1.5);scene.add(player);model=addBear();player.add(model);
 updateChunks();resize();addEventListener("resize",resize,{passive:true});
 ready=true;loading.style.display="none";say("Мир готов. Нажми «ЗАГРУЗИТЬ GLB» для моделей из ZIP.");
 requestAnimationFrame(loop);
}
function loop(){requestAnimationFrame(loop);const dt=Math.min(.045,clock.getDelta());if(!paused){anim+=dt;update(dt);updateChunks();cameraUpdate(dt);coinMeshes.forEach(c=>{if(c.mesh.visible){c.mesh.rotation.y+=dt*2;c.mesh.position.y=1.18+Math.sin(anim*3+c.mesh.position.z)*.12;}});}renderer.render(scene,camera);}
function importFiles(files){
 const arr=Array.from(files||[]).filter(f=>f.name.toLowerCase().endsWith(".glb"));if(!arr.length){say("Выбери файлы .glb");return;}
 const loader=new T.GLTFLoader();let left=arr.length,bears=0,maps=0;
 arr.forEach(file=>{const reader=new FileReader();reader.onload=e=>{loader.parse(e.target.result,"",gltf=>{
 const name=file.name.toLowerCase(),isBear=name.includes("12.x.x_model")||name==="bear.glb"||name.includes("player");
 if(isBear){replaceBear(gltf);bears++;}else{gltf.scene.traverse(o=>{if(o.isMesh)o.frustumCulled=true;});mapModels.push(gltf.scene);maps++;}
 left--;if(!left){refreshMaps();say("Готово: "+bears+" персонажей, "+maps+" карт");}
 },err=>{console.error(err);left--;if(!left)say("Часть GLB не загрузилась");});};reader.onerror=()=>{left--;if(!left)say("Ошибка чтения файла");};reader.readAsArrayBuffer(file);});
}
const joy=$("joystick"),stick=$("stick");
function stickMove(e){const r=joy.getBoundingClientRect(),mx=r.left+r.width/2,my=r.top+r.height/2,lim=r.width*.32;let dx=e.clientX-mx,dy=e.clientY-my,l=Math.hypot(dx,dy);if(l>lim){dx=dx/l*lim;dy=dy/l*lim;}stick.style.transform="translate("+dx+"px,"+dy+"px)";moveX=dx/lim;moveY=dy/lim;}
joy.addEventListener("pointerdown",e=>{stickId=e.pointerId;joy.setPointerCapture(e.pointerId);stickMove(e);e.preventDefault();});
joy.addEventListener("pointermove",e=>{if(stickId===e.pointerId)stickMove(e);});
function releaseJoy(e){if(stickId!==e.pointerId)return;stickId=null;moveX=moveY=0;stick.style.transform="translate(0,0)";}
joy.addEventListener("pointerup",releaseJoy);joy.addEventListener("pointercancel",releaseJoy);
canvas.addEventListener("pointerdown",e=>{if(e.clientX<innerWidth*.4)return;lookId=e.pointerId;lastLookX=e.clientX;canvas.setPointerCapture(e.pointerId);});
canvas.addEventListener("pointermove",e=>{if(lookId!==e.pointerId||paused)return;yaw=(yaw+(e.clientX-lastLookX)*.006)%(Math.PI*2);lastLookX=e.clientX;});
canvas.addEventListener("pointerup",e=>{if(lookId===e.pointerId)lookId=null;});canvas.addEventListener("pointercancel",e=>{if(lookId===e.pointerId)lookId=null;});
$("jump").addEventListener("pointerdown",e=>{e.preventDefault();jump();});$("jump").addEventListener("click",jump);
$("look").addEventListener("click",()=>{yaw=yaw===0?.48:0;say(yaw===0?"Камера сзади":"Камера сбоку");});
$("pause").addEventListener("click",()=>{paused=!paused;$("panel").style.display=paused?"flex":"none";});
$("resume").addEventListener("click",()=>{paused=false;$("panel").style.display="none";clock.getDelta();});
$("importModels").addEventListener("click",()=>$("modelFiles").click());
$("modelFiles").addEventListener("change",e=>{importFiles(e.target.files);e.target.value="";});
try{setup();}catch(error){console.error(error);loading.textContent='Не удалось запустить 3D-графику на этом устройстве.';}
})();