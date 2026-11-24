(function () {
"use strict";
const $ = id => document.getElementById(id);
const canvas = $("game");
const loading = $("loading");
const toastBox = $("toast");
const mapCanvas = $("map");
const mapCtx = mapCanvas.getContext("2d");
if (!window.THREE) {
  loading.innerHTML = "3D ENGINE ERROR<small>Не удалось загрузить локальный 3D-движок.</small>";
  return;
}
const T = window.THREE;
const clamp = (v, a, b) => Math.max(a, Math.min(b, v));
const lerp = (a, b, t) => a + (b - a) * t;
const rand = (a, b) => a + Math.random() * (b - a);
const dist2 = (a, b, x, z) => {
  const dx = a - x, dz = b - z;
  return dx * dx + dz * dz;
};
let renderer, scene, camera, clock;
let lastTime = 0, paused = false, ready = false;
let yaw = 0, pitch = 0.16, cameraDistance = 8.8;
let moveX = 0, moveY = 0, stickPointer = null;
let lookPointer = null, lastLookX = 0, lastLookY = 0;
let braking = false, nitro = false, inCar = false;
let player, players = [], activeCharacter = 0;
let playerPos = new T.Vector3(-173, 0, 105);
let heroPositions = [new T.Vector3(-173, 0, 105), new T.Vector3(-210, 0, 185)];
let car, carSpeed = 0, carYaw = 0, cash = 850;
let traffic = [], pedestrians = [], policeCars = [], buildings = [], buildingWindows = [];
let trafficClock = 0, npcClock = 0, missionStage = 0;
let wantedLevel = 0, wantedTimer = 0, policePulse = 0;
let dialogueQueue = [], dialogueIndex = 0, dialogueActive = false;
let missionMarker, missionRing, missionBeam, missionPulse = 0;
let toastTimer = 0, cameraTarget = new T.Vector3();
let sunLight, hemiLight, cityWindowMaterial, lampBulbs = [];
let roadMaterial, rainLines, rainPositions, rainMode = 0, gameTime = 0.36;
let waterWaves = [];
let fuel = 100, carCondition = 100, saveAccumulator = 0, lastCrashToast = 0, carPaintSaved = 0;
let lastAutoSave = 0;
let easterEggs = [], savedEggs = [], foundEggs = 0;
let raceActive = false, raceTimer = 0, raceIndex = 0, raceMarkers = [];
let raceBest = 0, raceStart = new T.Vector3(-500,0,210);
const worldSize = 1400;
const roadXs = Array.from({length:9},(_,i)=>-560+i*80);
const roads = Array.from({length:15},(_,i)=>-560+i*80);
const specialLots = [[-500,-520],[-160,-520],[-500,280],[-500,520]];
const missionData = [
  {
    x:-120,z:8,title:"Встреться с Рэй в кафе старого порта",district:"OLD PORT",hero:0,reward:250,
    scene:[
      {name:"КАЙ",text:"Ты сказала, что у тебя есть доказательство. Что происходит с портом?"},
      {name:"РЭЙ",text:"Его хотят закрыть через неделю. Людей выселяют, мастерские сносят, а документы прячут."},
      {name:"КАЙ",text:"Кто за этим стоит?"},
      {name:"РЭЙ",text:"Компания Meridian. Забери карту памяти из фотолаборатории на старом рынке. Там запись встречи."}
    ]
  },
  {
    x:120,z:-160,title:"Забери карту памяти из фотолаборатории",district:"OLD MARKET",hero:0,reward:450,
    scene:[
      {name:"ЛИНА · СООБЩЕНИЕ",text:"Кай, на карте есть запись. Но охрана уже заметила пропажу. Патруль едет к рынку."},
      {name:"КАЙ",text:"Понял. Отвезу карту в старый гараж. Не выходи на связь по обычному каналу."},
      {name:"РЭЙ · СООБЩЕНИЕ",text:"На карте не только запись. Там список сделок и имена людей, которые продали весь район."}
    ]
  },
  {
    x:-40,z:-240,title:"Уйди от патруля и доставь улику в гараж",district:"NORTH BAY",hero:0,reward:700,
    scene:[
      {name:"ЛИНА · СООБЩЕНИЕ",text:"Я вижу патруль на прибрежной дороге. Не стой на месте — встретимся в гараже."},
      {name:"КАЙ",text:"Они не получат эту запись. После встречи решим, как её обнародовать."},
      {name:"ЛИНА",text:"Я заберу вторую копию. Переключись на меня, когда будешь готов."}
    ]
  },
  {
    x:-220,z:200,title:"Переключись на Лину и встреться у мотеля",district:"MOTEL STRIP",hero:1,reward:550,
    scene:[
      {name:"ЛИНА",text:"Пока Кай отвлекал охрану, я нашла исходный архив. В нём видно, как подделали документы на выселение."},
      {name:"КАЙ · СООБЩЕНИЕ",text:"У нас есть всё необходимое. Но если просто выложить файлы, их назовут подделкой."},
      {name:"ЛИНА",text:"Тогда покажем запись в прямом эфире. Я знаю журналистку на радиостанции в центре."}
    ]
  },
  {
    x:40,z:40,title:"Передай архив журналистке на радиостанции",district:"DOWNTOWN",hero:1,reward:1500,
    scene:[
      {name:"ЖУРНАЛИСТКА",text:"Если запись настоящая, завтра об этом узнает весь город. Вы понимаете, что назад дороги не будет?"},
      {name:"ЛИНА",text:"Мы понимаем. Люди должны знать, почему их дома исчезают с карты."},
      {name:"КАЙ · СООБЩЕНИЕ",text:"Эфир начался. Сохрани копию архива — это только первая глава."},
      {name:"ЛИНА",text:"Порт ещё можно спасти. Теперь у нас есть шанс."}
    ]
  }
];
const colors = {
  asphalt: 0x273238, sidewalk: 0xb7b6aa, lane: 0xd9cda8,
  grass: 0x55685a, sand: 0xcab78d, sea: 0x236b83
};
const materialCache = new Map();
function material(color, roughness, metalness, extra) {
  const key = [color, roughness || 0.85, metalness || 0, extra || ""].join(":");
  if (!materialCache.has(key)) materialCache.set(key, new T.MeshStandardMaterial({
    color: color, roughness: roughness === undefined ? 0.85 : roughness,
    metalness: metalness || 0
  }));
  return materialCache.get(key);
}
function box(parent, w, h, d, color, x, y, z, roughness, metalness) {
  const mesh = new T.Mesh(
    new T.BoxGeometry(w, h, d),
    material(color, roughness, metalness)
  );
  mesh.position.set(x, y, z);
  parent.add(mesh);
  return mesh;
}
function group(parent, x, y, z) {
  const g = new T.Group();
  g.position.set(x || 0, y || 0, z || 0);
  if (parent) parent.add(g);
  return g;
}
function makeGrainTexture(baseColor) {
  const c = document.createElement("canvas");
  c.width = 128; c.height = 128;
  const ctx = c.getContext("2d");
  const hex = "#" + baseColor.toString(16).padStart(6,"0");
  ctx.fillStyle = hex;
  ctx.fillRect(0,0,128,128);
  for(let i=0;i<1700;i++){
    const v=Math.floor(rand(70,190));
    ctx.fillStyle="rgba("+v+","+v+","+v+","+(rand(0.025,0.11))+")";
    const x=Math.floor(rand(0,128)),y=Math.floor(rand(0,128));
    const s=rand(0.5,2.1);
    ctx.fillRect(x,y,s,s);
  }
  const tex=new T.CanvasTexture(c);
  tex.wrapS=T.RepeatWrapping;
  tex.wrapT=T.RepeatWrapping;
  tex.repeat.set(5,1);
  if(renderer&&renderer.capabilities&&renderer.capabilities.getMaxAnisotropy)
    tex.anisotropy=Math.min(4,renderer.capabilities.getMaxAnisotropy());
  return tex;
}
function setupSurfaceTextures() {
  roadMaterial=new T.MeshStandardMaterial({
    map:makeGrainTexture(0x273238), color:0xffffff, roughness:0.96, metalness:0.02
  });
}
function texturedBox(parent,w,h,d,mat,x,y,z) {
  const mesh=new T.Mesh(new T.BoxGeometry(w,h,d),mat);
  mesh.position.set(x,y,z);
  parent.add(mesh);
  return mesh;
}
function addGround() {
  const land=new T.Mesh(new T.PlaneGeometry(950,1420),material(0x65766a));
  land.rotation.x=-Math.PI/2;land.position.set(-220,-0.42,0);scene.add(land);
  const beach=new T.Mesh(new T.PlaneGeometry(70,1420),material(colors.sand,0.96));
  beach.rotation.x=-Math.PI/2;beach.position.set(225,-0.30,0);scene.add(beach);
  const sea=new T.Mesh(new T.PlaneGeometry(900,1600),new T.MeshStandardMaterial({color:colors.sea,roughness:0.28,metalness:0.18}));
  sea.rotation.x=-Math.PI/2;sea.position.set(720,-0.48,0);scene.add(sea);
  box(scene,20,0.22,1420,0x9c9b8a,184,-0.22,0,0.9);
  for(let i=0;i<72;i++){
    const z=-680+i*19;
    const wave=box(scene,rand(9,19),0.035,0.22,i%2?0x73b7c4:0xa1d5d7,266+rand(-6,6),-0.34,z,0.3);
    wave.rotation.y=rand(-0.12,0.12);
    wave.userData.phase=rand(0,Math.PI*2);
    wave.userData.baseX=wave.position.x;
    waterWaves.push(wave);
  }
}
function addRoads() {
  for(let i=0;i<roads.length;i++){
    const z=roads[i];
    texturedBox(scene,950,0.16,13,roadMaterial,-220,-0.19,z);
    box(scene,950,0.12,2.8,colors.sidewalk,-220,-0.24,z-8.2,0.98);
    box(scene,950,0.12,2.8,colors.sidewalk,-220,-0.24,z+8.2,0.98);
  }
  for(let i=0;i<roadXs.length;i++){
    const x=roadXs[i];
    texturedBox(scene,13,0.16,1420,roadMaterial,x,-0.19,0);
    box(scene,2.8,0.12,1420,colors.sidewalk,x-8.2,-0.24,0,0.98);
    box(scene,2.8,0.12,1420,colors.sidewalk,x+8.2,-0.24,0,0.98);
  }
  const geom=new T.BoxGeometry(0.22,0.035,4.8),mat=material(colors.lane,0.9);
  const marks=new T.InstancedMesh(geom,mat,roads.length*40+roadXs.length*60);
  const dummy=new T.Object3D();let n=0;
  for(let r=0;r<roads.length;r++)for(let i=0;i<40;i++){
    dummy.position.set(-660+i*23,-0.09,roads[r]);
    dummy.rotation.set(0,0,0);dummy.updateMatrix();marks.setMatrixAt(n++,dummy.matrix);
  }
  for(let r=0;r<roadXs.length;r++)for(let i=0;i<60;i++){
    dummy.position.set(roadXs[r],-0.09,-650+i*22);
    dummy.rotation.y=Math.PI/2;dummy.updateMatrix();marks.setMatrixAt(n++,dummy.matrix);
  }
  scene.add(marks);
}
function addCity() {
  const palette=[0xb7b4aa,0x9ba8a8,0xc2b6a2,0x77888d,0xd4c4ad,0x87969a,0xaea99c,0x687c84];
  for(let ix=0;ix<9;ix++)for(let iz=0;iz<15;iz++){
    const cx=-560+ix*80,cz=-560+iz*80;
    if(specialLots.some(p=>dist2(cx,cz,p[0],p[1])<82*82))continue;
    const downtown=Math.abs(cx)<110&&Math.abs(cz)<130;
    const count=downtown?2:(Math.random()>0.72?2:1);
    for(let b=0;b<count;b++){
      let placed=false;
      for(let attempt=0;attempt<10&&!placed;attempt++){
        const bw=rand(15,27),bd=rand(15,27);
        const bx=cx+(b===0?-13:14)+rand(-3,3),bz=cz+(count===1?0:(b===0?-12:12))+rand(-3,3);
        const candidate={x:bx-bw/2-1,z:bz-bd/2-1,w:bw+2,d:bd+2};
        const overlaps=buildings.some(old=>
          candidate.x<old.x+old.w+3&&candidate.x+candidate.w+3>old.x&&
          candidate.z<old.z+old.d+3&&candidate.z+candidate.d+3>old.z
        );
        if(overlaps)continue;
        let height=downtown?rand(25,66):rand(8,29);
        if(Math.random()>0.82)height=rand(38,76);
        if(ix===0||iz===0||iz===14)height*=0.7;
        addBuilding(bx,bz,bw,bd,height,palette[Math.floor(rand(0,palette.length))],Math.floor(rand(0,8)));
        placed=true;
      }
    }
  }
  const winGeo=new T.BoxGeometry(1.18,1.36,0.11);
  const winMat=new T.MeshStandardMaterial({color:0x183d4b,roughness:0.24,metalness:0.16,emissive:0x102936,emissiveIntensity:0.34});
  cityWindowMaterial=winMat;
  const windows=new T.InstancedMesh(winGeo,winMat,buildingWindows.length),o=new T.Object3D();
  for(let i=0;i<buildingWindows.length;i++){
    const v=buildingWindows[i];o.position.set(v.x,v.y,v.z);o.rotation.set(0,v.ry,0);o.scale.set(v.scaleX,1,1);o.updateMatrix();
    windows.setMatrixAt(i,o.matrix);windows.setColorAt(i,new T.Color(Math.random()>0.88?0xe3bd78:0x8db8c3));
  }
  scene.add(windows);
}

function addBuilding(x, z, w, d, h, color, style) {
  const g = group(scene, x, 0, z);
  const baseH = Math.min(3.2, h * 0.22);
  box(g, w, h, d, color, 0, h / 2 - 0.4, 0, 0.9);
  box(g, w + 0.28, 0.32, d + 0.28, 0x444c4b, 0, h - 0.48, 0, 0.78);
  box(g, w * 0.96, baseH, d * 0.985, 0x243b43, 0, baseH / 2 - 0.38, 0, 0.35, 0.18);
  if (style % 3 === 0 && h > 14) {
    for (let i = 0; i < 2; i++) {
      box(g, w * 0.72, 0.2, 0.55, 0x7b8584, 0, h + 0.04 + i * 0.55, -d * 0.16, 0.8);
    }
  }
  if (style % 4 === 1) {
    const sign = box(g, w * 0.42, 1.15, 0.16, [0xe9a64e,0x4da4ac,0xd66f5b][style % 3],
      0, 2.6, -d / 2 - 0.12, 0.4, 0.1);
    sign.userData.neon = true;
  }
  const rows = Math.max(2, Math.floor((h - baseH - 1) / 3.1));
  const frontCount = Math.max(2, Math.floor(w / 2.5));
  const sideCount = Math.max(2, Math.floor(d / 2.5));
  for (let r = 0; r < rows; r++) {
    const wy = baseH + 1.8 + r * 3.0;
    for (let c = 0; c < frontCount; c++) {
      const wx = -w / 2 + (c + 0.5) * w / frontCount;
      buildingWindows.push({x:x+wx,y:wy,z:z-d/2-0.08,ry:0,scaleX:0.68});
      buildingWindows.push({x:x+wx,y:wy,z:z+d/2+0.08,ry:Math.PI,scaleX:0.68});
    }
    for (let c = 0; c < sideCount; c++) {
      const wz = -d / 2 + (c + 0.5) * d / sideCount;
      buildingWindows.push({x:x-w/2-0.08,y:wy,z:z+wz,ry:-Math.PI/2,scaleX:0.68});
      buildingWindows.push({x:x+w/2+0.08,y:wy,z:z+wz,ry:Math.PI/2,scaleX:0.68});
    }
  }
  buildings.push({x:x-w/2-1,z:z-d/2-1,w:w+2,d:d+2,h:h});
  if (h > 23) {
    box(g, 2.8, 1.2, 2.8, 0x59636a, w * 0.2, h + 0.6, d * 0.12, 0.75);
  }
}
function addCity() {
  const palette = [0xb7b4aa,0x9ba8a8,0xc2b6a2,0x77888d,0xd4c4ad,0x87969a,0xaea99c,0x687c84];
  for (let ix = 0; ix < 7; ix++) {
    for (let iz = 0; iz < 7; iz++) {
      const cx = -240 + ix * 80;
      const cz = -240 + iz * 80;
      if (cx > 165) continue;
      const lotW = 57, lotD = 57;
      const count = (ix > 1 && ix < 5 && iz > 1 && iz < 5) ? 2 : (Math.random() > 0.55 ? 2 : 1);
      const downtown = Math.abs(cx) < 95 && Math.abs(cz) < 110;
      for (let b = 0; b < count; b++) {
        let placed = false;
        for(let attempt=0;attempt<10&&!placed;attempt++){
          const bw = Math.min(rand(15, 27),lotW), bd = Math.min(rand(15, 27),lotD);
          const bx = cx + (b === 0 ? -13 : 14) + rand(-3, 3);
          const bz = cz + (count === 1 ? 0 : (b === 0 ? -12 : 12)) + rand(-3, 3);
          const candidate={x:bx-bw/2-1,z:bz-bd/2-1,w:bw+2,d:bd+2};
          const overlaps=buildings.some(old=>
            candidate.x<old.x+old.w+3&&candidate.x+candidate.w+3>old.x&&
            candidate.z<old.z+old.d+3&&candidate.z+candidate.d+3>old.z
          );
          if(overlaps)continue;
          let height = downtown ? rand(25, 66) : rand(8, 29);
          if (Math.random() > 0.82) height = rand(38, 76);
          if (ix === 0 || iz === 0 || iz === 6) height *= 0.7;
          addBuilding(bx,bz,bw,bd,height,
            palette[Math.floor(rand(0,palette.length))],Math.floor(rand(0,8)));
          placed=true;
        }
      }
    }
  }
  const winGeo = new T.BoxGeometry(1.18, 1.36, 0.11);
  const winMat = new T.MeshStandardMaterial({color:0x183d4b,roughness:0.24,metalness:0.16,emissive:0x102936,emissiveIntensity:0.34});
  cityWindowMaterial=winMat;
  const windows = new T.InstancedMesh(winGeo, winMat, buildingWindows.length);
  const o = new T.Object3D();
  for (let i = 0; i < buildingWindows.length; i++) {
    const v = buildingWindows[i];
    o.position.set(v.x, v.y, v.z);
    o.rotation.set(0, v.ry, 0);
    o.scale.set(v.scaleX, 1, 1);
    o.updateMatrix();
    windows.setMatrixAt(i, o.matrix);
    const tint = new T.Color(Math.random() > 0.88 ? 0xe3bd78 : 0x8db8c3);
    windows.setColorAt(i, tint);
  }
  scene.add(windows);
}
function palm(x, z, scale) {
  const g = group(scene, x, 0, z);
  const trunk = new T.Mesh(new T.CylinderGeometry(0.16*scale,0.28*scale,4.8*scale,7), material(0x806344));
  trunk.position.y = 2.35*scale; trunk.rotation.z = rand(-0.08,0.08); g.add(trunk);
  for (let i = 0; i < 7; i++) {
    const leaf = new T.Mesh(new T.ConeGeometry(0.33*scale,3.7*scale,5), material(0x2e7053));
    leaf.position.set(Math.sin(i*Math.PI*2/7)*1.0*scale,4.6*scale,Math.cos(i*Math.PI*2/7)*1.0*scale);
    leaf.rotation.z = Math.cos(i*Math.PI*2/7)*1.05;
    leaf.rotation.x = Math.sin(i*Math.PI*2/7)*1.05;
    leaf.rotation.y = i*Math.PI*2/7;
    g.add(leaf);
  }
}
function streetLamp(x, z) {
  const g = group(scene, x, 0, z);
  const pole = new T.Mesh(new T.CylinderGeometry(0.075,0.11,5.2,7), material(0x394449,0.6,0.45));
  pole.position.y=2.6;g.add(pole);
  const arm=box(g,1.25,0.12,0.12,0x394449,0.48,5.05,0,0.55,0.45);
  arm.rotation.z=-0.18;
  const lampMat=new T.MeshStandardMaterial({color:0xffdf9b,emissive:0xffb54a,emissiveIntensity:0.08});
  const lamp=new T.Mesh(new T.BoxGeometry(0.6,0.16,0.38),lampMat);
  lamp.position.set(0.9,4.9,0);g.add(lamp);
  lampBulbs.push(lampMat);
}
function addStreetProps() {
  for(let i=0;i<60;i++){
    const z=-680+i*23;palm(174,z,rand(0.72,1.05));if(i%2===0)palm(244,z+8,rand(0.65,0.9));
  }
  for(let x=-520;x<=120;x+=160)for(let z=-560;z<=560;z+=160){streetLamp(x+10,z+12);streetLamp(x-10,z-12);}
  for(let i=0;i<9;i++){const x=198+i*7;box(scene,0.38,1.6,0.38,0x765d44,x,0.55,75,0.9);box(scene,0.42,0.13,0.42,0xd4bd91,x,1.35,75,0.9);}
  box(scene,54,0.7,16,0x806e56,226,-0.05,112,0.94);
  for(let i=0;i<7;i++){const z=76+i*11;box(scene,34,0.55,3.1,0x80634d,254,-0.1,z,0.9);for(let x=238;x<272;x+=15)box(scene,0.42,2.5,0.42,0x634d3d,x,-0.4,z,0.9);}
  for(let i=0;i<3;i++){const boat=group(scene,286,0,-4+i*32);box(boat,5.5,0.7,12,0xe3e5de,0,0.15,0,0.35,0.12);box(boat,3.2,1.2,4,0xf0eee5,0,0.9,-1,0.3,0.08);box(boat,2.5,0.08,5,0x346a7c,0,0.55,3,0.3);}
}
function addWorldSign(label,x,z,color){
  const c=document.createElement("canvas");c.width=512;c.height=128;const ctx=c.getContext("2d");
  ctx.fillStyle="#101d25";ctx.fillRect(0,0,512,128);ctx.fillStyle=color||"#f2c46a";ctx.fillRect(8,8,496,112);
  ctx.fillStyle="#13232b";ctx.fillRect(14,14,484,100);ctx.fillStyle=color||"#f2c46a";
  ctx.font="900 44px Arial";ctx.textAlign="center";ctx.textBaseline="middle";ctx.fillText(label,256,66);
  const tex=new T.CanvasTexture(c),mat=new T.MeshBasicMaterial({map:tex,transparent:true,side:T.DoubleSide});
  const sign=new T.Mesh(new T.PlaneGeometry(28,7),mat);sign.position.set(x,8,z);scene.add(sign);
}
function addDistrictLandmarks(){
  // Original airport district.
  box(scene,260,0.12,24,0x333d42,-475,-0.18,-520,0.94);
  for(let i=0;i<19;i++)box(scene,7,0.035,0.6,0xe8e4d6,-565+i*10,-0.08,-520,0.8);
  addBuilding(-540,-445,44,34,15,0x879397,2);addBuilding(-355,-510,18,18,46,0x78868b,3);
  box(scene,12,0.5,1.4,0xf0c45f,-355,47,-510,0.6);addWorldSign("BAYLINE AIRPORT",-480,-435,"#efc56f");
  // Stadium bowl.
  const bowl=new T.Mesh(new T.CylinderGeometry(50,54,12,40,1,true),new T.MeshStandardMaterial({color:0x657078,roughness:0.9,side:T.DoubleSide}));
  bowl.position.set(-160,5,-520);scene.add(bowl);
  const field=new T.Mesh(new T.PlaneGeometry(48,28),material(0x3c754e,0.95));field.rotation.x=-Math.PI/2;field.position.set(-160,0.25,-520);scene.add(field);
  const ring=new T.Mesh(new T.TorusGeometry(52,1.1,6,48),material(0xd4d8d0,0.45,0.2));ring.rotation.x=Math.PI/2;ring.position.set(-160,10.8,-520);scene.add(ring);
  buildings.push({x:-214,z:-574,w:108,d:108,h:12});addWorldSign("PALM BAY STADIUM",-160,-455,"#86d2ae");
  // Oval speedway.
  const track=new T.Mesh(new T.RingGeometry(39,53,64),new T.MeshStandardMaterial({color:0x313a3e,roughness:0.97,side:T.DoubleSide}));
  track.rotation.x=-Math.PI/2;track.position.set(-500,-0.12,280);scene.add(track);
  const outer=new T.Mesh(new T.TorusGeometry(53,0.8,5,64),material(0xe4e0d5,0.8));outer.rotation.x=Math.PI/2;outer.position.set(-500,0.1,280);scene.add(outer);
  const inner=new T.Mesh(new T.TorusGeometry(39,0.6,5,64),material(0xe4e0d5,0.8));inner.rotation.x=Math.PI/2;inner.position.set(-500,0.1,280);scene.add(inner);
  const infield=new T.Mesh(new T.CircleGeometry(35,32),material(0x52704f,0.95));infield.rotation.x=-Math.PI/2;infield.position.set(-500,-0.02,280);scene.add(infield);
  addWorldSign("BAYLINE SPEEDWAY",-500,215,"#f2a35c");
  // Cypress marsh pools and trees.
  const marsh=new T.Mesh(new T.CircleGeometry(90,32),material(0x566b56,0.98));marsh.rotation.x=-Math.PI/2;marsh.position.set(-500,-0.25,520);scene.add(marsh);
  for(let i=0;i<5;i++){const pond=new T.Mesh(new T.CircleGeometry(12+i%3*3,20),new T.MeshStandardMaterial({color:0x315d59,roughness:0.25,metalness:0.12}));pond.rotation.x=-Math.PI/2;pond.position.set(-555+i*27,-0.16,490+(i%2)*42);scene.add(pond);}
  for(let i=0;i<13;i++){const x=-575+rand(0,115),z=460+rand(-70,75);const trunk=new T.Mesh(new T.CylinderGeometry(0.18,0.3,5,7),material(0x61503a));trunk.position.set(x,2.5,z);scene.add(trunk);const crown=new T.Mesh(new T.ConeGeometry(2.4,6,7),material(0x315c47));crown.position.set(x,7,z);scene.add(crown);}
  addWorldSign("CYPRESS MARSH",-500,420,"#91c58a");
}

function createPerson(shirtColor, pantsColor) {
  const root=group(null,0,0,0);
  const shirt=material(shirtColor,0.88), pants=material(pantsColor,0.9), skin=material(0xd59d78,0.9), shoe=material(0x1e2428);
  const torso=new T.Mesh(new T.BoxGeometry(0.62,0.82,0.34),shirt);torso.position.y=1.15;root.add(torso);
  const head=new T.Mesh(new T.SphereGeometry(0.22,12,10),skin);head.position.set(0,1.76,0.01);root.add(head);
  const hair=new T.Mesh(new T.SphereGeometry(0.225,10,8),material(0x302a28));hair.scale.set(1,0.48,1);hair.position.set(0,1.89,-0.01);root.add(hair);
  const arms=[],legs=[];
  for(const s of [-1,1]){
    const arm=new T.Mesh(new T.BoxGeometry(0.18,0.65,0.2),shirt);
    arm.position.set(s*0.4,1.13,0);root.add(arm);arms.push(arm);
    const leg=new T.Mesh(new T.BoxGeometry(0.23,0.68,0.27),pants);
    leg.position.set(s*0.17,0.43,0);root.add(leg);legs.push(leg);
    const foot=new T.Mesh(new T.BoxGeometry(0.25,0.12,0.39),shoe);
    foot.position.set(s*0.17,0.1,0.07);root.add(foot);
  }
  const shadow=new T.Mesh(
    new T.CircleGeometry(0.62,18),
    new T.MeshBasicMaterial({color:0x0b1115,transparent:true,opacity:0.23,depthWrite:false})
  );
  shadow.rotation.x=-Math.PI/2;
  shadow.position.y=0.035;
  shadow.scale.set(1.0,1.5,1);
  root.add(shadow);
  root.userData={arms:arms,legs:legs,phase:rand(0,6)};
  return root;
}
function addPlayer() {
  const kai=createPerson(0x2f5964,0x252e38);
  kai.position.copy(heroPositions[0]);
  kai.scale.setScalar(1.02);
  kai.userData.heroName="KAI";
  const lina=createPerson(0x74516f,0x292c3b);
  lina.position.copy(heroPositions[1]);
  lina.scale.setScalar(1.02);
  lina.userData.heroName="LINA";
  players=[kai,lina];
  activeCharacter=clamp(activeCharacter,0,1);
  players.forEach((hero,i)=>{hero.visible=i===activeCharacter;});
  player=players[activeCharacter];
  playerPos.copy(heroPositions[activeCharacter]);
  scene.add(kai,lina);
  updateHeroLabel();
}
function updateHeroLabel() {
  const tag=$("heroTag");
  if(tag) tag.textContent=activeCharacter===0?"KAI · КАЙ":"LINA · ЛИНА";
}
function switchHero() {
  if(!ready||paused||inCar||dialogueActive)return;
  heroPositions[activeCharacter].copy(playerPos);
  player.visible=false;
  activeCharacter=1-activeCharacter;
  player=players[activeCharacter];
  playerPos.copy(heroPositions[activeCharacter]);
  player.position.copy(playerPos);
  player.visible=true;
  yaw=0;
  pitch=0.16;
  updateHeroLabel();
  saveGame();
  showToast(activeCharacter===0?"Кай вернулся в город.":"Лина подключилась из района мотелей.");
}
function createCar(color, scale) {
  const root=group(null,0,0,0);
  const bodyMat=material(color,0.3,0.28);
  const glassMat=new T.MeshStandardMaterial({
    color:0x173342,roughness:0.16,metalness:0.28,transparent:true,opacity:0.92
  });
  const rubber=material(0x11171b,0.96);
  const rim=material(0xbfc7c8,0.3,0.78);
  const body=box(root,2.02,0.56,4.25,color,0,0.72,0,0.3,0.28);
  box(root,1.66,0.17,3.7,0x262e32,0,0.43,0,0.75,0.2);
  // Cabin and separate tinted windows.
  box(root,1.64,0.76,2.22,0x263e49,0,1.32,-0.25,0.25,0.2);
  const frontGlass=box(root,1.48,0.48,0.08,0x173342,0,1.47,0.88,0.18,0.18);
  frontGlass.rotation.x=-0.19;frontGlass.material=glassMat;
  const rearGlass=box(root,1.48,0.46,0.08,0x173342,0,1.45,-1.36,0.18,0.18);
  rearGlass.rotation.x=0.2;rearGlass.material=glassMat;
  const sideWindowL=box(root,0.075,0.48,1.62,0x173342,-0.83,1.44,-0.25,0.18,0.18);
  const sideWindowR=box(root,0.075,0.48,1.62,0x173342,0.83,1.44,-0.25,0.18,0.18);
  sideWindowL.material=glassMat;sideWindowR.material=glassMat;
  box(root,0.1,0.62,1.78,0x294956,-0.82,1.32,-0.25,0.2,0.14);
  box(root,0.1,0.62,1.78,0x294956,0.82,1.32,-0.25,0.2,0.14);
  // Mirrors, handles, bumpers and front grille.
  for(const s of [-1,1]){
    box(root,0.23,0.12,0.22,color,s*0.98,1.42,0.38,0.3,0.25);
    box(root,0.07,0.09,0.46,0x252b2d,s*1.02,0.9,-0.25,0.5,0.35);
  }
  box(root,1.35,0.15,0.14,0x172126,0,0.66,2.14,0.6,0.4);
  box(root,1.7,0.12,0.48,0x202a2d,0,0.97,1.77,0.48);
  box(root,1.7,0.12,0.48,0x202a2d,0,0.97,-1.83,0.48);
  const headlights=[],brakelights=[],wheels=[];
  for(const s of [-1,1]){
    const headMat=new T.MeshStandardMaterial({color:0xfff3d0,emissive:0xffdf99,emissiveIntensity:0.7});
    const head=new T.Mesh(new T.BoxGeometry(0.48,0.17,0.07),headMat);
    head.position.set(s*0.66,0.86,2.15);root.add(head);headlights.push(head);
    const tailMat=new T.MeshStandardMaterial({color:0xd54135,emissive:0x6b130d,emissiveIntensity:0.35});
    const tail=new T.Mesh(new T.BoxGeometry(0.44,0.17,0.07),tailMat);
    tail.position.set(s*0.67,0.84,-2.16);root.add(tail);brakelights.push(tail);
    for(const z of [-1.36,1.35]){
      const wheelRoot=group(root,s*1.02,0.39,z);
      const tyre=new T.Mesh(new T.CylinderGeometry(0.39,0.39,0.28,16),rubber);
      tyre.rotation.z=Math.PI/2;wheelRoot.add(tyre);
      const hub=new T.Mesh(new T.CylinderGeometry(0.20,0.20,0.30,14),rim);
      hub.rotation.z=Math.PI/2;wheelRoot.add(hub);
      const cap=new T.Mesh(new T.CylinderGeometry(0.09,0.09,0.31,10),material(0x5f696b,0.3,0.8));
      cap.rotation.z=Math.PI/2;wheelRoot.add(cap);
      wheels.push(wheelRoot);
    }
  }
  const carShadow=new T.Mesh(
    new T.CircleGeometry(1.85,22),
    new T.MeshBasicMaterial({color:0x080d11,transparent:true,opacity:0.25,depthWrite:false})
  );
  carShadow.rotation.x=-Math.PI/2;
  carShadow.position.y=0.025;
  carShadow.scale.set(0.72,1.42,1);
  root.add(carShadow);
  root.scale.setScalar(scale||1);
  root.userData={wheelSpin:0,wheels:wheels,headlights:headlights,brakelights:brakelights,bodyMesh:body};
  return root;
}
function addVehicles() {
  car=createCar(0xb64e3d,1);
  car.position.set(-170,0,105);
  carYaw=0;
  scene.add(car);
  const palette=[0xe4e0d2,0x386f87,0x4c5154,0xc0a15e,0x7e3d39,0x9ba9a4,0x212a31,0xe1e0d7,0x799b87];
  for(let i=0;i<8;i++){
    const v=createCar(palette[i%palette.length],rand(0.72,0.9));
    const row=roads[(i*2+3)%roads.length];
    v.position.set(rand(-640,180),0.02,row+(i%2?2.8:-2.8));
    v.rotation.y=Math.PI/2;
    scene.add(v);
    traffic.push({mesh:v,speed:rand(5.5,10.5)*(i%3===0?1.3:1),lane:row,dir:i%2?1:-1});
  }
}
function addPolice() {
  for(let i=0;i<2;i++){
    const mesh=createCar(i===0?0x182b37:0x26343c,0.94);
    const red=new T.Mesh(new T.BoxGeometry(0.54,0.18,0.22),
      new T.MeshStandardMaterial({color:0xff3030,emissive:0xff1010,emissiveIntensity:0.8}));
    const blue=new T.Mesh(new T.BoxGeometry(0.54,0.18,0.22),
      new T.MeshStandardMaterial({color:0x287dff,emissive:0x145dff,emissiveIntensity:0.8}));
    red.position.set(-0.32,1.83,0);blue.position.set(0.32,1.83,0);
    mesh.add(red,blue);
    mesh.visible=false;
    scene.add(mesh);
    policeCars.push({mesh,red,blue,phase:i*1.7});
  }
}
function activateWanted(level) {
  wantedLevel=clamp(level,1,3);
  wantedTimer=0;
  const target=inCar?car.position:playerPos;
  policeCars.forEach((p,i)=>{
    p.mesh.position.set(
      clamp(target.x+(i===0?58:-64),-670,220),0,
      clamp(target.z+(i===0?48:-52),-670,670)
    );
    p.mesh.visible=true;
  });
  updateWantedHud();
  showToast("ВНИМАНИЕ: патруль заметил вас. Доберись до гаража.");
}
function clearWanted() {
  wantedLevel=0;
  wantedTimer=0;
  policeCars.forEach(p=>p.mesh.visible=false);
  updateWantedHud();
}
function updateWantedHud() {
  const el=$("wanted");
  if(!el)return;
  el.style.display=wantedLevel>0?"block":"none";
  el.textContent="ПАТРУЛЬ  "+"★".repeat(wantedLevel);
}
function addPedestrians() {
  const palette=[0x517c85,0x91664d,0xc0a15e,0x566b50,0x604c73,0xb5b4a9,0x8d4f47];
  for(let i=0;i<11;i++){
    const npc=createPerson(palette[i%palette.length],[0x29313a,0x35434a,0x66594b][i%3]);
    let placed=false;
    for(let attempt=0;attempt<24&&!placed;attempt++){
      const x=rand(-620,120),z=rand(-640,640);
      const clear=pedestrians.every(q=>dist2(x,z,q.position.x,q.position.z)>12)
        &&dist2(x,z,-120,8)>20
        &&dist2(x,z,heroPositions[0].x,heroPositions[0].z)>15
        &&dist2(x,z,heroPositions[1].x,heroPositions[1].z)>15;
      if(clear&&!hitsBuilding(x,z,0.8)){npc.position.set(x,0,z);placed=true;}
    }
    if(!placed)npc.position.set(rand(-600,100),0,rand(-620,620));
    npc.userData.walkX=rand(-1,1);
    npc.userData.walkZ=rand(-1,1);
    npc.userData.speed=rand(0.65,1.35);
    scene.add(npc);
    pedestrians.push(npc);
  }
  const contact=createPerson(0x8c493f,0x292b36);
  contact.position.set(-120,0,8);
  contact.scale.setScalar(1.03);
  scene.add(contact);
  pedestrians.push(contact);
}
function markerPosition() {
  const m=missionData[Math.min(missionStage,missionData.length-1)] || missionData[0];
  return new T.Vector3(m.x,0,m.z);
}
function createEasterEggs(){
  const data=[
    {x:-610,z:-470,name:"Утка-пилот",joke:"Утка-пилот: «Посадка прошла мягко. Это была трава»."},
    {x:-500,z:585,name:"Болотный гусь",joke:"Болотный гусь: «Кря — это мой бизнес-план»."},
    {x:-130,z:-455,name:"Стадионный пончик",joke:"Пончик: «Я круглый, но не хожу по кругу»."},
    {x:140,z:390,name:"Краб-блогер",joke:"Краб-блогер: «Подпишись. Я всё равно иду боком»."},
    {x:-620,z:95,name:"Радио 404",joke:"Радио 404: «Ваш любимый хит не найден. Ставим шум океана»."},
    {x:-330,z:210,name:"Мини-НЛО",joke:"Пилот НЛО: «Мы искали парковку. Тут везде платно»."}
  ];
  const colors=[0xe7bd43,0x7db7ad,0xd98c4b,0xe3a2c6,0x8bb7df,0xb8a5df];
  data.forEach((d,i)=>{
    const root=group(scene,d.x,0,d.z);
    const pedestal=new T.Mesh(new T.CylinderGeometry(1,1.2,0.45,12),material(0x3a464a,0.55,0.4));
    pedestal.position.y=0.23;root.add(pedestal);
    const ring=new T.Mesh(new T.TorusGeometry(1.18,0.1,6,20),
      new T.MeshStandardMaterial({color:0xffd36d,emissive:0xc78b28,emissiveIntensity:0.65}));
    ring.rotation.x=Math.PI/2;ring.position.y=0.48;root.add(ring);
    const gold=material(colors[i],0.34,0.48);
    if(i===0||i===1){
      const body=new T.Mesh(new T.SphereGeometry(0.64,14,11),gold);
      body.scale.set(1.1,0.75,1.25);body.position.set(0,1.02,0);root.add(body);
      const head=new T.Mesh(new T.SphereGeometry(0.38,12,10),gold);
      head.position.set(0.15,1.58,0.25);root.add(head);
      const beak=new T.Mesh(new T.ConeGeometry(0.18,0.5,8),material(0xe68b3f,0.65));
      beak.rotation.x=Math.PI/2;beak.position.set(0.15,1.48,0.62);root.add(beak);
      for(const side of [-1,1]){
        const wing=new T.Mesh(new T.SphereGeometry(0.32,9,8),gold);
        wing.scale.set(0.38,0.82,1);wing.position.set(side*0.62,1.04,-0.02);root.add(wing);
      }
    }else if(i===2){
      const donut=new T.Mesh(new T.TorusGeometry(0.56,0.24,10,28),gold);
      donut.rotation.x=Math.PI/2;donut.position.y=1.22;root.add(donut);
      box(root,0.12,0.05,0.28,0xff7194,0.22,1.42,0.1);
    }else if(i===3){
      const shell=new T.Mesh(new T.SphereGeometry(0.55,12,10),gold);
      shell.scale.set(1.25,0.68,1);shell.position.y=1.15;root.add(shell);
      for(const side of [-1,1]){
        const claw=new T.Mesh(new T.SphereGeometry(0.2,8,8),gold);
        claw.position.set(side*0.85,1.05,0.12);root.add(claw);
        const leg=box(root,0.12,0.12,0.72,0xa64d3b,side*0.6,0.55,0.1);leg.rotation.y=side*0.4;
      }
    }else if(i===4){
      box(root,1.05,0.7,0.46,0x374b57,0,1.15,0,0.45,0.35);
      box(root,0.58,0.32,0.08,0x6dd5c2,0,1.2,0.25,0.3,0.2);
      const antenna=new T.Mesh(new T.CylinderGeometry(0.025,0.035,0.85,6),material(0xb9c5c8));
      antenna.position.set(0.38,1.85,-0.12);antenna.rotation.z=-0.2;root.add(antenna);
    }else{
      const saucer=new T.Mesh(new T.CylinderGeometry(0.84,0.84,0.22,18),gold);
      saucer.position.y=1.2;root.add(saucer);
      const dome=new T.Mesh(new T.SphereGeometry(0.42,12,10,0,Math.PI*2,0,Math.PI/2),material(0x8bd6e8,0.18,0.5));
      dome.position.y=1.28;root.add(dome);
      for(let k=0;k<6;k++){
        const light=new T.Mesh(new T.SphereGeometry(0.07,7,7),new T.MeshBasicMaterial({color:k%2?0xffd36d:0x71dcff}));
        const a=k/6*Math.PI*2;light.position.set(Math.cos(a)*0.72,1.05,Math.sin(a)*0.72);root.add(light);
      }
    }
    scene.add(root);
    easterEggs.push({root,x:d.x,z:d.z,name:d.name,joke:d.joke,found:!!savedEggs[i]});
    if(savedEggs[i])root.visible=false;
  });
  updateEggCounter();
}
function updateEggCounter(){
  foundEggs=easterEggs.filter(e=>e.found).length;
  const el=$("eggCount");if(el)el.textContent=foundEggs+"/6";
}
function tryCollectEgg(){
  const p=inCar?car.position:playerPos;
  for(const e of easterEggs){
    if(e.found)continue;
    if(dist2(p.x,p.z,e.x,e.z)<64){
      e.found=true;e.root.visible=false;cash+=300;
      $("cash").textContent="$ "+cash.toLocaleString("en-US");
      updateEggCounter();saveGame();
      showToast("ПАСХАЛКА: "+e.name+" · "+e.joke+" · +$300");
      return true;
    }
  }
  return false;
}
function updateEasterEggs(dt){
  easterEggs.forEach((e,i)=>{
    if(!e.found&&e.root.visible){
      e.root.rotation.y+=dt*0.42;
      e.root.position.y=0.08+Math.sin(trafficClock*1.8+i)*0.10;
    }
  });
}
function addSpeedwayActivity(){
  const points=[{x:-460,z:280},{x:-500,z:320},{x:-540,z:280},{x:-500,z:240}];
  points.forEach(p=>{
    const g=group(scene,p.x,0,p.z);
    const ring=new T.Mesh(new T.TorusGeometry(3.0,0.18,6,20),
      new T.MeshStandardMaterial({color:0x66e3ff,emissive:0x1c8eaf,emissiveIntensity:0.75}));
    ring.rotation.x=Math.PI/2;ring.position.y=1.2;g.add(ring);
    const beam=new T.Mesh(new T.CylinderGeometry(0.1,0.1,4.8,8),
      new T.MeshBasicMaterial({color:0x65dfff,transparent:true,opacity:0.2}));
    beam.position.y=2.4;g.add(beam);scene.add(g);
    raceMarkers.push({root:g,x:p.x,z:p.z});
    g.visible=false;
  });
  const start=group(scene,raceStart.x,0,raceStart.z);
  const startRing=new T.Mesh(new T.TorusGeometry(4,0.22,7,24),
    new T.MeshStandardMaterial({color:0xffd36d,emissive:0xa46d18,emissiveIntensity:0.85}));
  startRing.rotation.x=Math.PI/2;startRing.position.y=0.5;start.add(startRing);
  const startBeam=new T.Mesh(new T.CylinderGeometry(0.12,0.12,5,8),
    new T.MeshBasicMaterial({color:0xffd36d,transparent:true,opacity:0.17}));
  startBeam.position.y=2.5;start.add(startBeam);scene.add(start);
}
function startSpeedwayRace(){
  if(!inCar){showToast("Для гонки сначала сядь в машину.");return;}
  if(dist2(car.position.x,car.position.z,raceStart.x,raceStart.z)>225){
    showToast("Старт гонки находится на Bayline Speedway — ищи золотую отметку.");
    return;
  }
  raceActive=true;raceTimer=0;raceIndex=0;
  raceMarkers.forEach((m,i)=>m.root.visible=i===0);
  $("raceStatus").style.display="block";
  showToast("BAYLINE SPEEDWAY · Пройди четыре голубые точки за 100 секунд!");
}
function updateRace(dt){
  if(!raceActive)return;
  raceTimer+=dt;
  const marker=raceMarkers[raceIndex];
  if(marker&&dist2(car.position.x,car.position.z,marker.x,marker.z)<100){
    marker.root.visible=false;raceIndex++;
    if(raceIndex>=raceMarkers.length){
      raceActive=false;cash+=1800;
      $("cash").textContent="$ "+cash.toLocaleString("en-US");
      $("raceStatus").style.display="none";
      raceBest=raceBest?Math.min(raceBest,raceTimer):raceTimer;
      showToast("ГОНКА ВЫИГРАНА · +$1800 · "+raceTimer.toFixed(1)+" сек.");
      raceMarkers.forEach(m=>m.root.visible=false);saveGame();return;
    }
    raceMarkers[raceIndex].root.visible=true;
    showToast("Контрольная точка "+raceIndex+"/4");
  }
  $("raceStatus").textContent="ГОНКА · "+(raceIndex+1)+"/4 · "+Math.max(0,100-raceTimer).toFixed(0)+" сек.";
  if(raceTimer>100){
    raceActive=false;raceMarkers.forEach(m=>m.root.visible=false);
    $("raceStatus").style.display="none";showToast("Время вышло. Попробуй ещё раз!");
  }
}
function createMissionMarker() {
  const g=group(scene,0,0,0);
  missionRing=new T.Mesh(new T.TorusGeometry(2.3,0.18,8,32),
    new T.MeshStandardMaterial({color:0xffc45f,emissive:0xb86e19,emissiveIntensity:0.7,metalness:0.2,roughness:0.3}));
  missionRing.rotation.x=Math.PI/2;missionRing.position.y=0.18;g.add(missionRing);
  missionBeam=new T.Mesh(new T.CylinderGeometry(0.16,1.1,6.7,16,1,true),
    new T.MeshBasicMaterial({color:0xffc45f,transparent:true,opacity:0.17,depthWrite:false}));
  missionBeam.position.y=3.45;g.add(missionBeam);
  const gem=new T.Mesh(new T.OctahedronGeometry(0.8,0),
    new T.MeshStandardMaterial({color:0xfff0c5,emissive:0xe6a73c,emissiveIntensity:1.0}));
  gem.position.y=7.1;g.add(gem);
  missionMarker=g;
  updateMissionMarker();
}
function updateMissionMarker() {
  if(!missionMarker)return;
  if(missionStage>=missionData.length){
    missionMarker.visible=false;
    $("objectiveText").textContent="Глава завершена — свободное исследование";
    $("district").textContent="FREE ROAM";
    return;
  }
  const p=markerPosition();
  missionMarker.position.set(p.x,0,p.z);
  missionMarker.visible=true;
  const data=missionData[missionStage];
  $("objectiveText").textContent=data.title;
  $("district").textContent=data.district;
}
function showToast(text) {
  toastBox.textContent=text;
  toastBox.style.opacity="1";
  clearTimeout(toastTimer);
  toastTimer=setTimeout(()=>toastBox.style.opacity="0",2600);
}
function beginStoryScene(index) {
  const mission=missionData[index];
  if(!mission)return;
  dialogueQueue=mission.scene.slice();
  dialogueIndex=0;
  dialogueActive=true;
  paused=true;
  $("dialoguePanel").style.display="flex";
  showStoryLine();
}
function showStoryLine() {
  const line=dialogueQueue[dialogueIndex];
  if(!line)return;
  $("speaker").textContent=line.name;
  $("dialogueText").textContent=line.text;
  $("dialogueCount").textContent=(dialogueIndex+1)+" / "+dialogueQueue.length;
}
function advanceStoryScene() {
  if(!dialogueActive)return;
  dialogueIndex++;
  if(dialogueIndex<dialogueQueue.length){showStoryLine();return;}
  dialogueActive=false;
  $("dialoguePanel").style.display="none";
  paused=false;
  lastTime=performance.now();
  const completed=missionStage;
  cash+=missionData[completed].reward;
  $("cash").textContent="$ "+cash.toLocaleString("en-US");
  if(completed===2)clearWanted();
  setStage(completed+1);
  if(completed===1)activateWanted(2);
  if(completed===missionData.length-1){
    showToast("ГЛАВА 1 ЗАВЕРШЕНА · +$ "+missionData[completed].reward+" · Город открыт");
  }
  saveGame();
}
function setStage(n) {
  missionStage=clamp(n,0,missionData.length);
  try{localStorage.setItem("neoncoast_stage",String(missionStage));}catch(e){}
  updateMissionMarker();
  if(missionStage>=missionData.length){
    clearWanted();
    showToast("Глава завершена! История сохранена — город открыт для исследования.");
  }else{
    showToast("Новое задание: "+missionData[missionStage].title);
  }
}
function doAction() {
  if(dialogueActive){advanceStoryScene();return;}
  if(!ready||paused)return;
  if(tryCollectEgg())return;
  if(raceActive){showToast("Продолжай гонку и проезжай через голубые маркеры.");return;}
  if(inCar&&dist2(car.position.x,car.position.z,raceStart.x,raceStart.z)<225){
    startSpeedwayRace();return;
  }
  if(inCar){
    const p=markerPosition();
    const d=dist2(car.position.x,car.position.z,p.x,p.z);
    if(missionStage<missionData.length&&d<220&&Math.abs(carSpeed)<2.2){
      const mission=missionData[missionStage];
      if(activeCharacter!==mission.hero){
        showToast("Для этого эпизода переключись на "+(mission.hero===0?"Кая":"Лину")+".");
        return;
      }
      beginStoryScene(missionStage);
      return;
    }
    if(Math.abs(carSpeed)>3){showToast("Сначала останови машину.");return;}
    const sideX=Math.cos(carYaw)*2.35,sideZ=-Math.sin(carYaw)*2.35;
    const candidates=[
      new T.Vector3(car.position.x+sideX,0,car.position.z+sideZ),
      new T.Vector3(car.position.x-sideX,0,car.position.z-sideZ)
    ];
    const exitPos=candidates.find(p=>
      p.x>-675&&p.x<220&&p.z>-675&&p.z<675&&
      !hitsBuilding(p.x,p.z,0.7)&&!hitsOtherVehicle(p.x,p.z,2.1)
    );
    if(!exitPos){showToast("Рядом слишком тесно, попробуй немного отъехать.");return;}
    inCar=false;
    player.visible=true;
    playerPos.copy(exitPos);
    heroPositions[activeCharacter].copy(playerPos);
    player.position.copy(playerPos);
    saveGame();
    updateHud();
    showToast("Ты вышел из машины.");
    return;
  }
  const cdist=dist2(playerPos.x,playerPos.z,car.position.x,car.position.z);
  if(cdist<36){
    inCar=true;player.visible=false;carYaw=car.rotation.y;
    showToast("Ты за рулём. Джойстик — газ и поворот, ТОРМОЗ и NITRO доступны.");
    updateHud();
    return;
  }
  if(missionStage<missionData.length){
    const mission=missionData[missionStage];
    const p=markerPosition();
    const d=dist2(playerPos.x,playerPos.z,p.x,p.z);
    if(d<220){
      if(activeCharacter!==mission.hero){
        showToast("Для этого эпизода переключись на "+(mission.hero===0?"Кая":"Лину")+".");
        return;
      }
      beginStoryScene(missionStage);
    }else showToast("Следуй к жёлтому маркеру на карте.");
  }else{
    showToast("История завершена. Можно свободно исследовать побережье.");
  }
}
function setupLighting() {
  scene.background=new T.Color(0x9ac0cc);
  scene.fog=new T.Fog(0x9ac0cc,190,540);
  hemiLight=new T.HemisphereLight(0xd8e9ee,0x59604d,1.28);
  scene.add(hemiLight);
  sunLight=new T.DirectionalLight(0xffe2bf,2.0);
  sunLight.position.set(-120,190,-90);
  scene.add(sunLight);
  const fill=new T.DirectionalLight(0x7da7c2,0.48);
  fill.position.set(150,70,180);scene.add(fill);
}
function updateEnvironment(dt) {
  gameTime=(gameTime+dt/520)%1;
  const angle=gameTime*Math.PI*2;
  const daylight=clamp((Math.sin(angle)+0.08)/1.08,0,1);
  const night=1-daylight;
  const daySky=new T.Color(0x9ac0cc);
  const nightSky=new T.Color(0x101b30);
  const dawnSky=new T.Color(0xd28c72);
  const blend=new T.Color();
  if(daylight<0.22){
    blend.copy(nightSky).lerp(dawnSky,daylight/0.22);
  }else{
    blend.copy(dawnSky).lerp(daySky,clamp((daylight-0.22)/0.78,0,1));
  }
  scene.background.copy(blend);
  scene.fog.color.copy(blend);
  scene.fog.near=rainMode?125:190;
  scene.fog.far=rainMode?390:540;
  sunLight.position.set(Math.cos(angle)*180,Math.max(8,Math.sin(angle)*190),-90);
  sunLight.intensity=0.14+daylight*1.95;
  sunLight.color.setRGB(1,0.76+daylight*0.19,0.60+daylight*0.38);
  hemiLight.intensity=0.32+daylight*0.98;
  if(cityWindowMaterial)cityWindowMaterial.emissiveIntensity=0.12+night*1.42;
  lampBulbs.forEach(m=>{m.emissiveIntensity=night*1.25;});
  if(roadMaterial)roadMaterial.color.setRGB(
    rainMode?0.78:1, rainMode?0.82:1, rainMode?0.86:1
  );
  const minutes=Math.floor(gameTime*24*60);
  const hh=String(Math.floor(minutes/60)%24).padStart(2,"0");
  const mm=String(minutes%60).padStart(2,"0");
  const timeEl=$("timeDisplay");
  if(timeEl)timeEl.textContent=hh+":"+mm+(rainMode?" · ДОЖДЬ":" · ЯСНО");
}
function createWeatherSystem() {
  const count=190;
  rainPositions=new Float32Array(count*2*3);
  for(let i=0;i<count;i++){
    const x=playerPos.x+rand(-34,34),y=rand(2,32),z=playerPos.z+rand(-34,34);
    const k=i*6;
    rainPositions[k]=x;rainPositions[k+1]=y;rainPositions[k+2]=z;
    rainPositions[k+3]=x+0.2;rainPositions[k+4]=y-1.1;rainPositions[k+5]=z+0.1;
  }
  const geo=new T.BufferGeometry();
  geo.setAttribute("position",new T.BufferAttribute(rainPositions,3));
  const mat=new T.LineBasicMaterial({color:0xa9d7ec,transparent:true,opacity:0.38});
  rainLines=new T.LineSegments(geo,mat);
  rainLines.visible=false;
  scene.add(rainLines);
}
function updateWater(dt) {
  for(let i=0;i<waterWaves.length;i++){
    const wave=waterWaves[i];
    const phase=wave.userData.phase;
    wave.position.y=-0.34+Math.sin(trafficClock*1.15+phase)*0.035;
    wave.position.x=wave.userData.baseX+Math.sin(trafficClock*0.42+phase)*1.8;
  }
}
function updateWeather(dt) {
  if(!rainLines||rainMode!==1)return;
  const target=inCar?car.position:playerPos;
  for(let i=0;i<rainPositions.length;i+=6){
    rainPositions[i]+=2.0*dt;
    rainPositions[i+1]-=27*dt;
    rainPositions[i+2]+=0.8*dt;
    if(rainPositions[i+1]<0){
      rainPositions[i]=target.x+rand(-34,34);
      rainPositions[i+1]=rand(17,34);
      rainPositions[i+2]=target.z+rand(-34,34);
    }
    rainPositions[i+3]=rainPositions[i]+0.2;
    rainPositions[i+4]=rainPositions[i+1]-1.1;
    rainPositions[i+5]=rainPositions[i+2]+0.1;
  }
  rainLines.geometry.attributes.position.needsUpdate=true;
}
function toggleWeather() {
  rainMode=rainMode===0?1:0;
  if(rainLines)rainLines.visible=rainMode===1;
  const button=$("weatherToggle");
  if(button)button.textContent=rainMode===1?"ПОГОДА: ДОЖДЬ · СМЕНИТЬ":"ПОГОДА: ЯСНО · СМЕНИТЬ";
  showToast(rainMode===1?"Начался дождь. Дорога стала скользкой.":"Дождь закончился.");
  saveGame();
}
function setupSkyline() {
  for(let i=0;i<32;i++){
    const h=rand(28,95),w=rand(8,20),d=rand(8,18);
    const m=new T.Mesh(new T.BoxGeometry(w,h,d),material([0x778c91,0x87979a,0x9aa4a2][i%3]));
    const angle=i/32*Math.PI*2;
    m.position.set(Math.cos(angle)*360,h/2-0.4,Math.sin(angle)*360);
    scene.add(m);
  }
}
function setupRenderer() {
  renderer=new T.WebGLRenderer({canvas:canvas,antialias:true,alpha:false,powerPreference:"high-performance"});
  renderer.setPixelRatio(Math.min(window.devicePixelRatio||1,1.15));
  renderer.setSize(window.innerWidth,window.innerHeight,false);
  renderer.outputEncoding=T.sRGBEncoding;
  renderer.toneMapping=T.ACESFilmicToneMapping;
  renderer.toneMappingExposure=1.05;
}
function resize() {
  if(!renderer||!camera)return;
  const w=window.innerWidth,h=window.innerHeight;
  renderer.setPixelRatio(Math.min(window.devicePixelRatio||1,1.25));
  renderer.setSize(w,h,false);
  camera.aspect=w/h;camera.updateProjectionMatrix();
}
function hitsPedestrian(x,z,r,ignore) {
  for(let i=0;i<pedestrians.length;i++){
    const p=pedestrians[i];
    if(p===ignore||!p.visible)continue;
    if(dist2(x,z,p.position.x,p.position.z)<r*r)return true;
  }
  return false;
}
function hitsOtherVehicle(x,z,r) {
  const list=traffic.concat(policeCars);
  for(let i=0;i<list.length;i++){
    const mesh=list[i].mesh;
    if(!mesh.visible)continue;
    if(dist2(x,z,mesh.position.x,mesh.position.z)<r*r)return true;
  }
  return false;
}
function updatePlayer(dt) {
  const inputLen=Math.sqrt(moveX*moveX+moveY*moveY);
  if(inputLen>0.05){
    const ix=moveX/inputLen,iy=moveY/inputLen;
    const dx=ix*Math.cos(yaw)+iy*Math.sin(yaw);
    const dz=-ix*Math.sin(yaw)+iy*Math.cos(yaw);
    const speed=6.1;
    const nx=playerPos.x+dx*speed*dt;
    const nz=playerPos.z+dz*speed*dt;
    if(nx>-680&&nx<225&&nz>-680&&nz<680&&
       !hitsBuilding(nx,nz,0.62)&&!hitsPedestrian(nx,nz,1.05,player)&&
       !hitsOtherVehicle(nx,nz,1.35)&&dist2(nx,nz,car.position.x,car.position.z)>1.8){
      playerPos.x=nx;playerPos.z=nz;
    }
    player.rotation.y=Math.atan2(dx,dz);
    const phase=performance.now()*0.0105;
    player.userData.arms.forEach((a,i)=>a.rotation.x=Math.sin(phase+i*Math.PI)*0.48);
    player.userData.legs.forEach((a,i)=>a.rotation.x=Math.sin(phase+i*Math.PI)*0.58);
  } else {
    player.userData.arms.forEach(a=>a.rotation.x=0);
    player.userData.legs.forEach(a=>a.rotation.x=0);
  }
  player.position.copy(playerPos);
  heroPositions[activeCharacter].copy(playerPos);
}
function hitsBuilding(x,z,r) {
  for(let i=0;i<buildings.length;i++){
    const b=buildings[i];
    if(x>b.x-r&&x<b.x+b.w+r&&z>b.z-r&&z<b.z+b.d+r)return true;
  }
  return false;
}
function updateCar(dt) {
  const throttle=fuel>0?moveY:Math.min(0,moveY);
  const steer=moveX;
  const boost=nitro&&!braking&&throttle>0&&fuel>0;
  const accel=boost?22:12.5;
  if(throttle>0.06)carSpeed+=accel*throttle*dt;
  else if(throttle< -0.06)carSpeed-=9.5*(-throttle)*dt;
  else carSpeed*=Math.pow(0.94,dt*10);
  if(braking)carSpeed*=Math.pow(0.48,dt*10);
  const roadLimit=carCondition<25?14:26;
  carSpeed=clamp(carSpeed,-8,boost?Math.min(34,roadLimit+5):roadLimit);
  const steerPower=clamp(Math.abs(carSpeed)/15,0.08,1.18)*(rainMode?0.78:1);
  carYaw+=steer*steerPower*dt*(carSpeed>=0?1:-1)*1.38;
  const nx=car.position.x+Math.sin(carYaw)*carSpeed*dt;
  const nz=car.position.z+Math.cos(carYaw)*carSpeed*dt;
  const blocked=hitsBuilding(nx,nz,1.2)||hitsOtherVehicle(nx,nz,3.5)||
    nx<-680||nx>225||nz<-680||nz>680;
  if(!blocked){
    car.position.x=nx;car.position.z=nz;
  }else{
    const impact=Math.abs(carSpeed);
    carSpeed*=-0.12;
    carCondition=clamp(carCondition-Math.max(1,impact*0.10),0,100);
    if(performance.now()-lastCrashToast>1800){
      showToast("Столкновение · состояние машины "+Math.round(carCondition)+"%");
      lastCrashToast=performance.now();
    }
  }
  car.rotation.y=carYaw;
  car.userData.wheelSpin+=carSpeed*dt*2.5;
  car.userData.wheels.forEach(w=>{w.rotation.x+=carSpeed*dt*1.7;});
  car.userData.headlights.forEach(h=>{h.material.emissiveIntensity=0.45+(1-clamp((Math.sin(gameTime*Math.PI*2)+0.08)/1.08,0,1))*1.45;});
  car.userData.brakelights.forEach(l=>{l.material.emissiveIntensity=braking?2.1:0.35;});
  if(Math.abs(carSpeed)>1.2){
    fuel=clamp(fuel-dt*(0.012+Math.abs(carSpeed)*0.0012),0,100);
  }
  if(fuel<=0)carSpeed*=Math.pow(0.86,dt*10);
  if(player)player.position.set(car.position.x,0,car.position.z);
  $("speedValue").textContent=String(Math.round(Math.abs(carSpeed)*3.6));
}
function updateTraffic(dt) {
  trafficClock+=dt;
  for(let i=0;i<traffic.length;i++){
    const t=traffic[i];
    t.mesh.position.x+=t.speed*t.dir*dt;
    if(t.mesh.position.x>230)t.mesh.position.x=-680;
    if(t.mesh.position.x< -680)t.mesh.position.x=230;
    t.mesh.position.z=t.lane+(i%2?2.8:-2.8);
  }
}
function updatePolice(dt) {
  if(wantedLevel<=0)return;
  policePulse+=dt;
  const target=inCar?car.position:playerPos;
  let nearest=9999;
  policeCars.forEach((p,i)=>{
    if(!p.mesh.visible)return;
    const dx=target.x-p.mesh.position.x,dz=target.z-p.mesh.position.z;
    const distance=Math.sqrt(dx*dx+dz*dz);
    nearest=Math.min(nearest,distance);
    const angle=Math.atan2(dx,dz);
    p.mesh.rotation.y=angle;
    if(distance>5){
      const speed=10+wantedLevel*2.8;
      p.mesh.position.x+=Math.sin(angle)*speed*dt;
      p.mesh.position.z+=Math.cos(angle)*speed*dt;
    }
    const flash=Math.sin(policePulse*11+p.phase)>0;
    p.red.material.emissiveIntensity=flash?1.8:0.12;
    p.blue.material.emissiveIntensity=flash?0.12:1.8;
  });
  wantedTimer+=dt;
  if(nearest>105&&wantedTimer>48){
    clearWanted();
    showToast("Патруль потерял след. Погоня окончена.");
  }
}
function updatePedestrians(dt) {
  npcClock+=dt;
  for(let i=0;i<pedestrians.length;i++){
    const p=pedestrians[i];
    if(i===pedestrians.length-1)continue;
    if(Math.random()<0.006){
      p.userData.walkX=rand(-1,1);p.userData.walkZ=rand(-1,1);
    }
    const nx=p.position.x+p.userData.walkX*p.userData.speed*dt;
    const nz=p.position.z+p.userData.walkZ*p.userData.speed*dt;
    let crowded=false;
    for(let j=0;j<pedestrians.length;j++){
      if(i===j)continue;
      const q=pedestrians[j];
      if(dist2(nx,nz,q.position.x,q.position.z)<2.4)crowded=true;
    }
    const nearCar=dist2(nx,nz,car.position.x,car.position.z)<8;
    if(nx>-650&&nx<145&&nz>-660&&nz<660&&
       !hitsBuilding(nx,nz,0.6)&&!crowded&&!nearCar&&
       !hitsOtherVehicle(nx,nz,1.8)){
      p.position.x=nx;p.position.z=nz;
    }else{
      p.userData.walkX=-p.userData.walkX;
      p.userData.walkZ=-p.userData.walkZ;
    }
    if(Math.abs(p.userData.walkX)+Math.abs(p.userData.walkZ)>0.1){
      p.rotation.y=Math.atan2(p.userData.walkX,p.userData.walkZ);
      const phase=npcClock*5+p.userData.phase;
      p.userData.arms.forEach((a,k)=>a.rotation.x=Math.sin(phase+k*Math.PI)*0.30);
      p.userData.legs.forEach((a,k)=>a.rotation.x=Math.sin(phase+k*Math.PI)*0.43);
    }
  }
}
function updateCamera(dt) {
  const target=inCar?car.position:playerPos;
  const followYaw=inCar?carYaw:yaw;
  let d=inCar?10.4:cameraDistance;
  let h=inCar?5.0:4.3+pitch*2;
  cameraTarget.set(target.x, target.y+1.5, target.z);
  let desired=new T.Vector3(
    target.x-Math.sin(followYaw)*d,
    target.y+h,
    target.z-Math.cos(followYaw)*d
  );
  if(hitsBuilding(desired.x,desired.z,0.65)){
    d*=0.58;h+=1.15;
    desired.set(target.x-Math.sin(followYaw)*d,target.y+h,
      target.z-Math.cos(followYaw)*d);
    if(hitsBuilding(desired.x,desired.z,0.55)){
      desired.set(target.x,target.y+7.4,target.z+1.2);
    }
  }
  camera.position.lerp(desired,1-Math.pow(0.001,dt));
  const look=new T.Vector3(
    target.x+Math.sin(followYaw)*2.4,
    target.y+1.35,
    target.z+Math.cos(followYaw)*2.4
  );
  camera.lookAt(look);
}
function updateMarker(dt) {
  if(!missionMarker||!missionMarker.visible)return;
  missionPulse+=dt;
  missionRing.rotation.z+=dt*0.75;
  missionRing.scale.setScalar(1+Math.sin(missionPulse*2.8)*0.13);
  missionBeam.material.opacity=0.11+Math.sin(missionPulse*2.1)*0.055;
  missionMarker.children[2].rotation.y+=dt*1.4;
}
function saveGame() {
  try{
    heroPositions[activeCharacter].copy(playerPos);
    const data={
      stage:missionStage,cash:cash,fuel:fuel,condition:carCondition,
      active:activeCharacter,time:gameTime,rain:rainMode,
      paint:car&&car.userData?car.userData.paintIndex||0:0,
      vehicle:car?[car.position.x,car.position.z,carYaw]:null,
      eggs:easterEggs.map(e=>e.found?1:0),
      heroes:heroPositions.map(p=>[p.x,p.y,p.z])
    };
    localStorage.setItem("neoncoast_save_v3",JSON.stringify(data));
  }catch(e){}
}
function saveOnExit() {
  saveGame();
}
function loadProgress() {
  try{
    const raw=localStorage.getItem("neoncoast_save_v3");
    if(raw){
      const d=JSON.parse(raw);
      if(Number.isFinite(d.stage))missionStage=clamp(Math.floor(d.stage),0,missionData.length);
      if(Number.isFinite(d.cash))cash=clamp(d.cash,0,9999999);
      if(Number.isFinite(d.fuel))fuel=clamp(d.fuel,0,100);
      if(Number.isFinite(d.condition))carCondition=clamp(d.condition,0,100);
      if(Number.isFinite(d.active))activeCharacter=clamp(Math.floor(d.active),0,1);
      if(Number.isFinite(d.time))gameTime=((d.time%1)+1)%1;
      if(Number.isFinite(d.rain))rainMode=d.rain?1:0;
      if(Number.isFinite(d.paint))carPaintSaved=clamp(Math.floor(d.paint),0,6);
      if(Array.isArray(d.eggs))savedEggs=d.eggs.slice(0,6).map(v=>v?1:0);
      if(Array.isArray(d.vehicle)&&d.vehicle.length>=3&&d.vehicle.every(Number.isFinite)){
        car.position.set(clamp(d.vehicle[0],-675,215),0,clamp(d.vehicle[1],-675,675));
        carYaw=d.vehicle[2];car.rotation.y=carYaw;
      }
      if(Array.isArray(d.heroes)&&d.heroes.length===2){
        d.heroes.forEach((v,i)=>{
          if(Array.isArray(v)&&v.length>=3&&v.every(Number.isFinite)){
            heroPositions[i].set(clamp(v[0],-675,215),0,clamp(v[2],-675,675));
          }
        });
      }
      playerPos.copy(heroPositions[activeCharacter]);
    }else{
      const n=parseInt(localStorage.getItem("neoncoast_stage")||"0",10);
      missionStage=(Number.isFinite(n)&&n>=0&&n<=missionData.length)?n:0;
    }
  }catch(e){missionStage=0;activeCharacter=0;}
  $("cash").textContent="$ "+cash.toLocaleString("en-US");
}
function drawMap() {
  const ctx=mapCtx,w=mapCanvas.width,h=mapCanvas.height;
  const mapX=x=>(x+700)/1400*w,mapZ=z=>(z+700)/1400*h;
  ctx.clearRect(0,0,w,h);
  ctx.fillStyle="#142c36";ctx.fillRect(0,0,w,h);
  ctx.fillStyle="#1e6274";ctx.fillRect(mapX(260),0,w-mapX(260),h);
  ctx.fillStyle="#b9a77e";ctx.fillRect(mapX(190),0,mapX(260)-mapX(190),h);
  ctx.strokeStyle="#69777a";ctx.lineWidth=2;
  for(let i=0;i<roadXs.length;i++){
    const p=mapX(roadXs[i]);ctx.beginPath();ctx.moveTo(p,0);ctx.lineTo(p,h);ctx.stroke();
  }
  for(let i=0;i<roads.length;i++){
    const p=mapZ(roads[i]);ctx.beginPath();ctx.moveTo(0,p);ctx.lineTo(mapX(260),p);ctx.stroke();
  }
  ctx.font="bold 8px Arial";ctx.fillStyle="#dce7ddaa";
  ctx.fillText("AIRPORT",mapX(-590),mapZ(-620));
  ctx.fillText("DOWNTOWN",mapX(-180),mapZ(15));
  ctx.fillText("SPEEDWAY",mapX(-565),mapZ(205));
  ctx.fillText("MARSH",mapX(-580),mapZ(635));
  const pos=inCar?car.position:playerPos,px=mapX(pos.x),pz=mapZ(pos.z);
  if(missionStage<missionData.length){
    const m=missionData[missionStage],mx=mapX(m.x),mz=mapZ(m.z);
    ctx.strokeStyle="#f5c45f";ctx.lineWidth=2;ctx.setLineDash([4,4]);
    ctx.beginPath();ctx.moveTo(px,pz);ctx.lineTo(mx,mz);ctx.stroke();ctx.setLineDash([]);
    ctx.fillStyle="#ffc65c";ctx.beginPath();ctx.arc(mx,mz,5,0,Math.PI*2);ctx.fill();
  }
  if(wantedLevel>0)policeCars.forEach(p=>{
    if(!p.mesh.visible)return;ctx.fillStyle="#ff6767";ctx.beginPath();
    ctx.arc(mapX(p.mesh.position.x),mapZ(p.mesh.position.z),3,0,Math.PI*2);ctx.fill();
  });
  easterEggs.forEach(e=>{if(!e.found){ctx.fillStyle="#f2ce69";ctx.fillRect(mapX(e.x)-2,mapZ(e.z)-2,4,4);}});
  if(raceActive&&raceMarkers[raceIndex]){
    const m=raceMarkers[raceIndex];ctx.fillStyle="#75e6ff";ctx.beginPath();ctx.arc(mapX(m.x),mapZ(m.z),4,0,Math.PI*2);ctx.fill();
  }
  ctx.fillStyle="#fff";ctx.strokeStyle="#15242a";ctx.lineWidth=3;
  ctx.beginPath();ctx.arc(px,pz,5,0,Math.PI*2);ctx.fill();ctx.stroke();
}

function updateHud() {
  $("speedValue").textContent=inCar?String(Math.round(Math.abs(carSpeed)*3.6)):"0";
  const info=$("vehicleInfo");
  if(info){
    info.style.display=inCar?"block":"none";
    info.textContent="ТОПЛИВО "+Math.round(fuel)+"%   ·   СОСТОЯНИЕ "+Math.round(carCondition)+"%";
  }
}
function loop(now) {
  requestAnimationFrame(loop);
  if(!ready||paused)return;
  const dt=Math.min(0.045,Math.max(0.001,(now-lastTime)/1000||0.016));
  lastTime=now;
  if(inCar)updateCar(dt);else updatePlayer(dt);
  updateTraffic(dt);
  updatePedestrians(dt);
  updatePolice(dt);
  updateEnvironment(dt);
  updateWater(dt);
  updateWeather(dt);
  updateCamera(dt);
  updateMarker(dt);
  updateEasterEggs(dt);
  updateRace(dt);
  saveAccumulator+=dt;
  if(saveAccumulator>12){saveAccumulator=0;saveGame();}
  drawMap();
  updateHud();
  renderer.render(scene,camera);
}
function updateStick(e) {
  const r=$("joystick").getBoundingClientRect();
  const cx=r.left+r.width/2,cy=r.top+r.height/2;
  const max=r.width*0.34;
  const dx=e.clientX-cx,dy=e.clientY-cy;
  const len=Math.sqrt(dx*dx+dy*dy)||1;
  const f=Math.min(1,max/len);
  const sx=dx*f,sy=dy*f;
  $("stick").style.transform="translate("+sx+"px,"+sy+"px)";
  moveX=clamp(sx/max,-1,1);
  moveY=clamp(-sy/max,-1,1);
}
function resetStick() {
  stickPointer=null;moveX=0;moveY=0;
  $("stick").style.transform="translate(0,0)";
}
function bindButton(id,down,up) {
  const el=$(id);
  el.addEventListener("pointerdown",e=>{e.preventDefault();el.setPointerCapture(e.pointerId);if(down)down();});
  const stop=e=>{e.preventDefault();if(up)up();};
  el.addEventListener("pointerup",stop);
  el.addEventListener("pointercancel",stop);
  el.addEventListener("lostpointercapture",stop);
}
function setupControls() {
  const joy=$("joystick");
  joy.addEventListener("pointerdown",e=>{e.preventDefault();stickPointer=e.pointerId;joy.setPointerCapture(e.pointerId);updateStick(e);});
  joy.addEventListener("pointermove",e=>{if(e.pointerId===stickPointer){e.preventDefault();updateStick(e);}});
  joy.addEventListener("pointerup",e=>{if(e.pointerId===stickPointer)resetStick();});
  joy.addEventListener("pointercancel",resetStick);
  canvas.addEventListener("pointerdown",e=>{
    if(e.clientX>window.innerWidth*0.42){lookPointer=e.pointerId;lastLookX=e.clientX;lastLookY=e.clientY;canvas.setPointerCapture(e.pointerId);}
  });
  canvas.addEventListener("pointermove",e=>{
    if(e.pointerId!==lookPointer)return;
    const dx=e.clientX-lastLookX,dy=e.clientY-lastLookY;
    lastLookX=e.clientX;lastLookY=e.clientY;
    yaw-=dx*0.006;
    pitch=clamp(pitch+dy*0.0015,-0.05,0.75);
  });
  const clearLook=()=>{lookPointer=null;};
  canvas.addEventListener("pointerup",clearLook);
  canvas.addEventListener("pointercancel",clearLook);
  bindButton("action",doAction);
  bindButton("brake",()=>{braking=true;},()=>{braking=false;});
  bindButton("nitro",()=>{nitro=true;},()=>{nitro=false;});
  $("switchHero").addEventListener("click",switchHero);
  $("dialogueNext").addEventListener("click",advanceStoryScene);
  $("phoneBtn").addEventListener("click",()=>{
    if(dialogueActive||paused)return;
    paused=true;
    $("phonePanel").style.display="flex";
    const mission=missionData[Math.min(missionStage,missionData.length-1)];
    $("phoneObjective").textContent=missionStage>=missionData.length?
      "Глава завершена. Берег открыт для исследования." : mission.title;
    $("weatherToggle").textContent=rainMode===1?"ПОГОДА: ДОЖДЬ · СМЕНИТЬ":"ПОГОДА: ЯСНО · СМЕНИТЬ";
  });
  $("weatherToggle").addEventListener("click",toggleWeather);
  $("paintBtn").addEventListener("click",()=>{
    if(dist2(playerPos.x,playerPos.z,car.position.x,car.position.z)>100&&!inCar){
      showToast("Подойди к своей машине, чтобы сменить цвет.");
      return;
    }
    const palette=[0xb64e3d,0x246b91,0xc2a04e,0x343b42,0x43816e,0xe0ddd3,0x713e68];
    const index=(car.userData.paintIndex||0)+1;
    car.userData.paintIndex=index%palette.length;
    car.userData.bodyMesh.material=material(palette[car.userData.paintIndex],0.3,0.28);
    showToast("Цвет машины изменён.");
    saveGame();
  });
  $("serviceBtn").addEventListener("click",()=>{
    const nearCar=inCar||dist2(playerPos.x,playerPos.z,car.position.x,car.position.z)<100;
    if(!nearCar){showToast("Подойди к своей машине для обслуживания.");return;}
    if(cash<150){showToast("Не хватает денег на обслуживание.");return;}
    cash-=150;fuel=100;carCondition=100;
    $("cash").textContent="$ "+cash.toLocaleString("en-US");
    updateHud();saveGame();
    showToast("Машина обслужена · $150");
  });
  $("phoneClose").addEventListener("click",()=>{
    $("phonePanel").style.display="none";
    paused=false;lastTime=performance.now();
  });
  $("pause").addEventListener("click",()=>{
    paused=true;$("panel").style.display="flex";
  });
  $("resume").addEventListener("click",()=>{
    $("panel").style.display="none";paused=false;lastTime=performance.now();
  });
  window.addEventListener("pagehide",saveOnExit);
  document.addEventListener("visibilitychange",()=>{
    if(document.hidden){
      if(!dialogueActive&&!paused){paused=true;$("panel").style.display="flex";}
      saveOnExit();
    }
  });
}
function init() {
  try {
    setupRenderer();
    scene=new T.Scene();
    setupLighting();
    setupSurfaceTextures();
    camera=new T.PerspectiveCamera(63,window.innerWidth/window.innerHeight,0.1,900);
    clock=new T.Clock();
    addGround();
    addRoads();
    addCity();
    addStreetProps();
    addDistrictLandmarks();
    addSpeedwayActivity();
    setupSkyline();
    addVehicles();
    loadProgress();
    if(carPaintSaved>0){
      const paintPalette=[0xb64e3d,0x246b91,0xc2a04e,0x343b42,0x43816e,0xe0ddd3,0x713e68];
      car.userData.paintIndex=carPaintSaved;
      car.userData.bodyMesh.material=material(paintPalette[carPaintSaved],0.3,0.28);
    }
    addPolice();
    addPedestrians();
    createWeatherSystem();
    addPlayer();
    createMissionMarker();
    createEasterEggs();
    updateEggCounter();
    updateMissionMarker();
    if(rainMode===1)rainLines.visible=true;
    setupControls();
    window.addEventListener("resize",resize);
    ready=true;
    loading.style.display="none";
    lastTime=performance.now();
    showToast("NEON COAST · Сохранившийся мир загружен. Кнопка телефона открывает настройки погоды и машины.");
    updateEnvironment(0);
    updateHud();
    requestAnimationFrame(loop);
  } catch(err) {
    loading.innerHTML="ОШИБКА ЗАПУСКА<small>"+String(err&&err.message?err.message:err)+"</small>";
    console.error(err);
  }
}
init();
})();