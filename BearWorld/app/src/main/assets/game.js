(function(){
"use strict";
const $=id=>document.getElementById(id),canvas=$("game"),loading=$("loading"),toast=$("toast");
if(!window.THREE||!THREE.GLTFLoader){loading.textContent="3D-движок не загрузился.";return;}
const T=THREE,CHUNK=36,SPEED=8.1,GRAVITY=19,MAX_DPR=1.3;
let renderer,scene,camera,clock,player,model,parts={},velocityY=0,onGround=true,paused=false,ready=false;
let yaw=0,coins=0,dist=0,moveX=0,moveY=0,stickId=null,lookId=null,lastLookX=0,anim=0,toastTimer=0;
let chunks=new Map(),coinMeshes=[],obstacles=[],mapModels=[];
const themes=[
 {ground:0x6c9954,path:0x91b86a,sky:0x8bc7e7,prop:0x477341},
 {ground:0xc2a15c,path:0xe0c47a,sky:0xf0bd78,prop:0x927041},
 {ground:0xb8d1d8,path:0xe9f5f5,sky:0x9ebfd7,prop:0x718c99},
 {ground:0x4b505e,path:0x737b8b,sky:0x303444,prop:0x555c70}
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
 model=gltf.scene;model.scale.setScalar(.82);model.rotation.y=Math.PI;
 model.traverse(o=>{if(o.isMesh)o.frustumCulled=true;});
 player.add(model);parts={};
 ["arm_l_4","arm_r_10","leg_l_18","leg_r_22"].forEach(n=>{const o=model.getObjectByName(n);if(o)parts[n]=o;});
}
function addTree(g,x,z,theme,s){
 const trunk=new T.Mesh(new T.CylinderGeometry(.15*s,.22*s,1.25*s,7),mat(0x6b4a31));trunk.position.set(x,.6*s,z);g.add(trunk);
 const col=theme===2?0xe4f1f3:theme===1?0x9b854a:theme===3?0x555c70:0x477a42;
 const crown=new T.Mesh(new T.ConeGeometry(.9*s,1.8*s,7),mat(col));crown.position.set(x,1.7*s,z);g.add(crown);
}
function addCoin(g,x,z,list){
 const m=new T.Mesh(new T.TorusGeometry(.3,.09,8,16),new T.MeshStandardMaterial({color:0xffd34f,metalness:.65,roughness:.28,emissive:0x442800}));
 m.position.set(x,1.18,z);m.rotation.y=Math.PI/2;g.add(m);list.push({mesh:m,taken:false});
}
function addRock(g,x,z,theme,s){
 const m=new T.Mesh(new T.DodecahedronGeometry(.7*s,0),mat([0x75816b,0x9b8054,0x9daeb7,0x6b7181][theme]));
 m.position.set(x,.35*s,z);m.rotation.z=.18;g.add(m);
}
function attachMap(entry){
 if(!mapModels.length||entry.mapGroup)return;
 const source=mapModels[Math.abs(entry.index)%mapModels.length],group=new T.Group(),clone=source.clone(true);
 clone.scale.setScalar(.24);clone.position.set(entry.index%2?11:-11,.1,-CHUNK*.5);
 clone.rotation.y=entry.index%3?0:Math.PI;group.add(clone);entry.group.add(group);entry.mapGroup=group;
}
function refreshMaps(){
 chunks.forEach(entry=>{if(entry.mapGroup)entry.group.remove(entry.mapGroup);entry.mapGroup=null;attachMap(entry);});
}
function makeChunk(index){
 const r=rngFor(index+12345),themeIndex=Math.floor(Math.abs(index)/4)%4,th=themes[themeIndex],g=new T.Group();
 g.position.z=-index*CHUNK;scene.add(g);
 const floor=new T.Mesh(new T.BoxGeometry(38,.75,CHUNK),mat(th.ground));floor.position.set(0,-.43,-CHUNK/2);g.add(floor);
 const path=new T.Mesh(new T.BoxGeometry(7.2,.16,CHUNK),mat(th.path));path.position.set(0,.015,-CHUNK/2);g.add(path);
 [-3.8,3.8].forEach(x=>{const rail=new T.Mesh(new T.BoxGeometry(.2,.38,CHUNK),mat(th.prop));rail.position.set(x,.15,-CHUNK/2);g.add(rail);});
 for(let i=0;i<6;i++){let z=-3-i*5.7;if(r()>.4)addTree(g,(r()>.5?1:-1)*(7+r()*7),z,themeIndex,.7+r()*.55);if(r()>.6)addRock(g,(r()>.5?1:-1)*(6+r()*6),z-2,themeIndex,.6+r()*.6);}
 const localCoins=[],localObs=[],lanes=[-2.25,0,2.25];
 for(let i=0;i<4;i++){let z=-6-i*7.2;if(r()>.2)addCoin(g,lanes[Math.floor(r()*3)],z,localCoins);
 if(i>0&&r()>.35){const o=new T.Mesh(new T.DodecahedronGeometry(.72,0),mat(th.prop));o.position.set(lanes[Math.floor(r()*3)],.55,z-2);o.scale.set(1.1,.8,.9);g.add(o);localObs.push({mesh:o,hit:false});}}
 const entry={index,group:g,coins:localCoins,obstacles:localObs,mapGroup:null};chunks.set(index,entry);
 coinMeshes.push(...localCoins);obstacles.push(...localObs);attachMap(entry);
}
function updateChunks(){
 const cur=Math.floor(-player.position.z/CHUNK),min=cur-3,max=cur+7;
 for(let i=min;i<=max;i++)if(!chunks.has(i))makeChunk(i);
 for(const [i,e] of Array.from(chunks.entries()))if(i<min||i>max){scene.remove(e.group);e.coins.forEach(c=>c.mesh.visible=false);e.obstacles.forEach(o=>o.mesh.visible=false);chunks.delete(i);}
 coinMeshes=coinMeshes.filter(c=>c.mesh.parent&&c.mesh.visible);obstacles=obstacles.filter(o=>o.mesh.parent&&o.mesh.visible);
 const t=themes[Math.floor(Math.abs(cur)/4)%4];scene.background.setHex(t.sky);scene.fog.color.setHex(t.sky);
}
function collectAndCollide(){
 const v=new T.Vector3();
 for(const c of coinMeshes){if(c.taken||!c.mesh.visible)continue;c.mesh.getWorldPosition(v);
 const dx=v.x-player.position.x,dz=v.z-player.position.z,dy=v.y-player.position.y-.9;
 if(dx*dx+dz*dz<1.5&&Math.abs(dy)<1.6){c.taken=true;c.mesh.visible=false;coins++;$("coins").textContent=coins;}}
 for(const o of obstacles){if(o.hit||!o.mesh.visible)continue;o.mesh.getWorldPosition(v);
 if(Math.abs(v.x-player.position.x)<1.15&&Math.abs(v.z-player.position.z)<1.15&&player.position.y<.9){o.hit=true;say("Препятствие! Перепрыгни его.");player.position.z+=1.7;velocityY=2.7;onGround=false;}}
}
function jump(){if(!ready||paused||!onGround)return;velocityY=8.2;onGround=false;}
function update(dt){
 let x=moveX,y=moveY;
 if(Math.abs(x)>1||Math.abs(y)>1){const l=Math.hypot(x,y);x/=l;y/=l;}
 player.position.x=T.MathUtils.clamp(player.position.x+x*SPEED*dt,-3,3);
 player.position.z+=y*SPEED*dt;
 if(!onGround){velocityY-=GRAVITY*dt;player.position.y+=velocityY*dt;if(player.position.y<=0){player.position.y=0;velocityY=0;onGround=true;}}
 const walking=Math.abs(x)+Math.abs(y)>.12;
 if(model){const s=Math.sin(anim*10)*.45;
 if(parts.arm_l_4)parts.arm_l_4.rotation.x=walking?s:0;
 if(parts.arm_r_10)parts.arm_r_10.rotation.x=walking?-s:0;
 if(parts.leg_l_18)parts.leg_l_18.rotation.x=walking?-s:0;
 if(parts.leg_r_22)parts.leg_r_22.rotation.x=walking?s:0;
 if(Math.abs(x)>.08)model.rotation.z+=(x>0?-.18:.18-model.rotation.z)*Math.min(1,dt*4);
 else model.rotation.z+=(0-model.rotation.z)*Math.min(1,dt*4);}
 collectAndCollide();dist=Math.max(0,Math.floor(Math.abs(player.position.z)*1.5));$("distance").textContent=dist;
}
function cameraUpdate(dt){
 const target=new T.Vector3(player.position.x+Math.sin(yaw)*9.1,player.position.y+4.9,player.position.z+Math.cos(yaw)*9.1);
 camera.position.lerp(target,Math.min(1,dt*4));camera.lookAt(player.position.x,player.position.y+1.0,player.position.z-2);
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
 clock=new T.Clock();player=new T.Group();player.position.set(0,0,2.4);scene.add(player);model=addBear();player.add(model);
 makeChunk(0);updateChunks();resize();addEventListener("resize",resize,{passive:true});
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
canvas.addEventListener("pointermove",e=>{if(lookId!==e.pointerId||paused)return;yaw=T.MathUtils.clamp(yaw+(e.clientX-lastLookX)*.004,-1.1,1.1);lastLookX=e.clientX;});
canvas.addEventListener("pointerup",e=>{if(lookId===e.pointerId)lookId=null;});canvas.addEventListener("pointercancel",e=>{if(lookId===e.pointerId)lookId=null;});
$("jump").addEventListener("pointerdown",e=>{e.preventDefault();jump();});$("jump").addEventListener("click",jump);
$("look").addEventListener("click",()=>{yaw=yaw===0?.48:0;say(yaw===0?"Камера сзади":"Камера сбоку");});
$("pause").addEventListener("click",()=>{paused=!paused;$("panel").style.display=paused?"flex":"none";});
$("resume").addEventListener("click",()=>{paused=false;$("panel").style.display="none";clock.getDelta();});
$("importModels").addEventListener("click",()=>$("modelFiles").click());
$("modelFiles").addEventListener("change",e=>{importFiles(e.target.files);e.target.value="";});
setup();
})();