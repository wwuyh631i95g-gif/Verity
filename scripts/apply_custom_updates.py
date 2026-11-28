from pathlib import Path

ROOT=Path("Verity-main")
JAVA=ROOT/"app/src/main/java/dev/veriti/app"
DATA=JAVA/"data"

def rep(path,old,new,label):
    s=path.read_text(encoding="utf-8")
    n=s.count(old)
    if n!=1: raise SystemExit(f"{label}: expected one match, got {n}")
    path.write_text(s.replace(old,new,1),encoding="utf-8")

models=DATA/"Models.kt"
rep(models,'    val selectedCharacterAsset: String = "",\n','    val selectedCharacterAsset: String = "",\n    val characterDisplayMode: String = "skins",\n',"appearance mode")
store=DATA/"ChatStore.kt"
rep(store,'        selectedCharacterAsset = prefs.getString("selectedCharacterAsset", "") ?: "",\n','        selectedCharacterAsset = prefs.getString("selectedCharacterAsset", "") ?: "",\n        characterDisplayMode = prefs.getString("characterDisplayMode", if ((prefs.getString("selectedCharacterAsset", "") ?: "").isNotBlank()) "catalog" else if ((prefs.getString("customAvatarUri", "") ?: "").isNotBlank()) "avatar" else "skins") ?: "skins",\n',"load mode")
rep(store,'            .putString("selectedCharacterAsset", value.selectedCharacterAsset)\n','            .putString("selectedCharacterAsset", value.selectedCharacterAsset)\n            .putString("characterDisplayMode", value.characterDisplayMode)\n',"save mode")

# Select one of three appearance systems without clearing the other two.
p=JAVA/"service/FloatingAssistantService.kt"
s=p.read_text(encoding="utf-8")
a=s.index("    private fun setCharacterImage(")
b=s.index("    private fun updateTextShake()",a)
s=s[:a]+'''    private fun setCharacterImage(fallback: Int, moodOverride: String? = null) {
        val mood = moodOverride ?: when (fallback) {
            R.drawable.verity_angry_cutout -> "angry"
            R.drawable.verity_sad_talking_cutout -> "sad"
            R.drawable.verity_normal_cutout -> "normal"
            R.drawable.verity_ouch_cutout -> "ouch"
            else -> "happy"
        }
        val settings = chatStore.loadSettings()
        val mode = settings.characterDisplayMode
        val avatar = if (mode == "avatar") SkinSheet.loadAvatar(this, settings.customAvatarUri) else null
        fun applyTo(target: ImageView, slot: Int, selectedMood: String) {
            val asset = if (mode == "catalog") dev.veriti.app.data.CharacterSlotAssets.get(settings,slot).ifBlank { settings.selectedCharacterAsset } else ""
            val catalog = if (asset.isNotBlank()) SkinSheet.loadCatalogSprite(this,asset,selectedMood) else null
            val alternate = slot > 0 && slot % 2 == 0 && settings.secondarySkinStyle != "none"
            val chosen = if (alternate) settings.copy(verityPlus=true,skinStyle=settings.secondarySkinStyle) else settings
            when {
                catalog != null -> target.setImageBitmap(catalog)
                slot == 0 && avatar != null -> {
                    val rounded=RoundedBitmapDrawableFactory.create(resources,avatar).apply { isCircular=true; setAntiAlias(true) }
                    target.setImageDrawable(rounded)
                }
                else -> {
                    val skin=SkinSheet.faceForMood(this,chosen,selectedMood)
                    if (skin != null) target.setImageBitmap(skin) else target.setImageResource(fallback)
                }
            }
        }
        applyTo(character,0,mood)
        extraCharacters.forEachIndexed { i,view ->
            val moods=listOf("normal","angry","sad","talking","ouch","scary","happy","curious","sleepy","laughing","surprised","love")
            applyTo(view,i+1,moods[i % moods.size])
        }
    }

''' + s[b:]
p.write_text(s,encoding="utf-8")

main=JAVA/"MainActivity.kt"
patches=[
('private enum class Screen { CHAT, GAME, SETTINGS, PLUS, ABOUT, SKIN_EDITOR, CHARACTERS, APP_BUILDER, HOME_ASSISTANT, FRIENDS, FEATURES }','private enum class Screen { CHAT, GAME, SETTINGS, PLUS, CUSTOMIZATIONS, ABOUT, SKIN_EDITOR, CHARACTERS, APP_BUILDER, HOME_ASSISTANT, FRIENDS, FEATURES }'),
('if (screen != Screen.ABOUT && screen != Screen.SKIN_EDITOR && screen != Screen.CHARACTERS) {','if (screen != Screen.ABOUT && screen != Screen.SKIN_EDITOR && screen != Screen.CHARACTERS && screen != Screen.CUSTOMIZATIONS) {'),
('vm.updateSettings(state.settings.copy(customAvatarUri = imported, selectedCharacterAsset = ""))','vm.updateSettings(state.settings.copy(customAvatarUri = imported, characterDisplayMode = "avatar"))'),
('vm.updateSettings(CharacterSlotAssets.set(state.settings, characterSlotTarget, assetPath).copy(customAvatarUri = ""))','vm.updateSettings(CharacterSlotAssets.set(state.settings, characterSlotTarget, assetPath).copy(characterDisplayMode = "catalog"))'),
('                onOpenSkinEditor = { screen = Screen.SKIN_EDITOR }\n            )\n            Screen.ABOUT','                onOpenSkinEditor = { screen = Screen.SKIN_EDITOR },\n                onOpenCustomizations = { screen = Screen.CUSTOMIZATIONS }\n            )\n            Screen.CUSTOMIZATIONS -> MinecraftCustomizationsScreen(onBack = { screen = Screen.PLUS })\n            Screen.ABOUT'),
('vm.updateSettings(state.settings.copy(skinStyle = "custom", customSkinSheetUri = uri))','vm.updateSettings(state.settings.copy(skinStyle = "custom", customSkinSheetUri = uri, characterDisplayMode = "skins"))'),
('val updated = draft.copy(customAvatarUri = imported, selectedCharacterAsset = "")','val updated = draft.copy(customAvatarUri = imported, characterDisplayMode = "avatar")'),
('applyOverlaySettings(draft.copy(customAvatarUri = ""))','applyOverlaySettings(draft.copy(customAvatarUri = "", characterDisplayMode = "skins"))'),
('applyOverlaySettings(draft.copy(selectedCharacterAsset = ""))','applyOverlaySettings(draft.copy(selectedCharacterAsset = "", characterDisplayMode = "skins"))'),
('applyOverlaySettings(draft.copy(skinStyle = "custom", customSkinSheetUri = uri, customAvatarUri = "", selectedCharacterAsset = ""))','applyOverlaySettings(draft.copy(skinStyle = "custom", customSkinSheetUri = uri, characterDisplayMode = "skins"))'),
('if (draft.customSkinSheetUri == uri) applyOverlaySettings(draft.copy(skinStyle = "verity", customSkinSheetUri = ""))','if (draft.customSkinSheetUri == uri) applyOverlaySettings(draft.copy(skinStyle = "verity", customSkinSheetUri = "", characterDisplayMode = "skins"))'),
('TextButton(onClick = { applyOverlaySettings(draft.copy(skinStyle = "verity", customSkinSheetUri = "")) }) { Text("Вернуть стандартный образ Verity") }','TextButton(onClick = { applyOverlaySettings(draft.copy(skinStyle = "verity", customSkinSheetUri = "", characterDisplayMode = "skins")) }) { Text("Вернуть стандартный образ Verity") }'),
('val updated = draft.copy(skinStyle = "custom", customSkinSheetUri = imported)','val updated = draft.copy(skinStyle = "custom", customSkinSheetUri = imported, characterDisplayMode = "skins")'),
('OutlinedButton(onClick = { apply(draft.copy(skinStyle = value)) }) { Text(label) }','OutlinedButton(onClick = { apply(draft.copy(skinStyle = value, characterDisplayMode = "skins")) }) { Text(label) }'),
]
for old,new in patches: rep(main,old,new,"MainActivity")
rep(main,'''private fun PlusScreen(
    settings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onStyleChange: (String) -> Unit,
    onOpenSkinEditor: () -> Unit
)''','''private fun PlusScreen(
    settings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onStyleChange: (String) -> Unit,
    onOpenSkinEditor: () -> Unit,
    onOpenCustomizations: () -> Unit
)''',"PlusScreen signature")
rep(main,'                item { SectionTitle("ИНТЕРФЕЙС") }','''                item {
                    SettingsCard {
                        Text("Кастомизации",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                        Text("Создавай наборы кнопок, устанавливай JSON-наборы и делись своими.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick=onOpenCustomizations,modifier=Modifier.fillMaxWidth()){Text("Открыть кастомизации")}
                    }
                }
                item { SectionTitle("ИНТЕРФЕЙС") }''',"Premium customizations button")

(DATA/"MinecraftButtonMods.kt").write_text(r'''package dev.veriti.app.data
import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
object MinecraftButtonMods {
 data class Mod(val id:String,val name:String,val color:Int,val shape:String,val symbol:String,val description:String="",val builtin:Boolean=false)
 private const val P="veriti_data"; private const val K="minecraft_button_mods"; private const val A="minecraftButtonModActive"
 private val presets=listOf(Mod("classic","Классический",0xA5222222.toInt(),"round","","Стандартный",true),Mod("gold","Золотой",0xD0A87924.toInt(),"round","✦","Золотые кнопки",true),Mod("neon","Неоновый",0xC0006877.toInt(),"neon","✧","Бирюзовая подсветка",true),Mod("violet","Фиолетовый",0xC05B3B88.toInt(),"square","◆","Угловатые кнопки",true))
 fun list(c:Context):List<Mod>{ val saved=runCatching{val a=JSONArray(c.getSharedPreferences(P,0).getString(K,"[]")?:"[]");buildList{for(i in 0 until a.length()){val o=a.optJSONObject(i)?:continue;val id=o.optString("id");val n=o.optString("name").trim();if(id.isNotBlank()&&n.isNotBlank())add(Mod(id,n.take(40),o.optInt("color",0xA5222222.toInt()),o.optString("shape").takeIf{it in setOf("round","square","neon")}?:"round",o.optString("symbol").take(4),o.optString("description").take(120)))}}}.getOrDefault(emptyList());return presets+saved.sortedBy{it.name.lowercase()} }
 fun activeId(c:Context)=c.getSharedPreferences(P,0).getString(A,"")?:""
 fun add(c:Context,m:Mod):Mod{val item=m.copy(id=UUID.randomUUID().toString(),name=m.name.trim().take(40),symbol=m.symbol.take(4),builtin=false);val p=c.getSharedPreferences(P,0);val saved=list(c).filter{!it.builtin}+item;p.edit().putString(K,JSONArray().apply{saved.forEach{put(json(it))}}.toString()).apply();return item}
 fun setActive(c:Context,m:Mod){c.getSharedPreferences(P,0).edit().putString(A,m.id).putInt("minecraftButtonModColor",m.color).putString("minecraftButtonModShape",m.shape).putString("minecraftButtonModSymbol",m.symbol).putString("minecraftButtonModName",m.name).apply()}
 fun disable(c:Context){c.getSharedPreferences(P,0).edit().putString(A,"").remove("minecraftButtonModColor").remove("minecraftButtonModShape").remove("minecraftButtonModSymbol").remove("minecraftButtonModName").apply()}
 fun delete(c:Context,id:String){val p=c.getSharedPreferences(P,0);val kept=list(c).filter{!it.builtin&&it.id!=id};p.edit().putString(K,JSONArray().apply{kept.forEach{put(json(it))}}.toString()).apply();if(activeId(c)==id)disable(c)}
 fun exportJson(m:Mod)=json(m.copy(builtin=false)).toString(2)
 fun importUri(c:Context,u:Uri):Mod?=runCatching{val raw=c.contentResolver.openInputStream(u)?.bufferedReader()?.use{it.readText()}?:return null;val o=JSONObject(raw);val n=o.optString("name").trim().take(40);if(n.isBlank())return null;val v=o.optString("color","#A5222222");val color=if(v.startsWith("#"))android.graphics.Color.parseColor(v)else v.toLong().toInt();add(c,Mod("",n,color,o.optString("shape","round"),o.optString("symbol").take(4),o.optString("description").take(120)))}.getOrNull()
 private fun json(m:Mod)=JSONObject().put("format","verity-button-mod-v1").put("id",m.id).put("name",m.name).put("color",String.format("#%08X",m.color)).put("shape",m.shape).put("symbol",m.symbol).put("description",m.description)
}
''',encoding="utf-8")

(JAVA/"MinecraftCustomizationsScreen.kt").write_text(r'''package dev.veriti.app
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.veriti.app.data.MinecraftButtonMods
private val modColors=listOf("Золото" to 0xD0A87924.toInt(),"Бирюза" to 0xC0006877.toInt(),"Фиолетовый" to 0xC05B3B88.toInt(),"Красный" to 0xC0A52D38.toInt(),"Синий" to 0xC02B5F9A.toInt(),"Тёмный" to 0xA5222222.toInt())
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun MinecraftCustomizationsScreen(onBack:()->Unit){
 val c=LocalContext.current;var mods by remember{mutableStateOf(MinecraftButtonMods.list(c))};var active by remember{mutableStateOf(MinecraftButtonMods.activeId(c))};var query by remember{mutableStateOf("")};var create by remember{mutableStateOf(false)};var message by remember{mutableStateOf("")}
 fun refresh(){mods=MinecraftButtonMods.list(c);active=MinecraftButtonMods.activeId(c)}
 val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){u->if(u!=null){val m=MinecraftButtonMods.importUri(c,u);refresh();message=if(m!=null)"Набор установлен" else "Не удалось прочитать JSON"}}
 val filtered=mods.filter{query.isBlank()||it.name.contains(query,true)||it.description.contains(query,true)||it.symbol.contains(query,true)}
 Scaffold(contentWindowInsets=WindowInsets(0),topBar={TopAppBar(modifier=Modifier.statusBarsPadding(),title={Text("Кастомизации",fontWeight=FontWeight.Bold)},navigationIcon={IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Rounded.ArrowBack,"Назад")}})}){pad->
  Column(Modifier.fillMaxSize().padding(pad).navigationBarsPadding().padding(horizontal=16.dp)){
   Text("Встроенные и установленные наборы. Общей онлайн-библиотеки пока нет: свои работы можно передавать JSON-файлом.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(vertical=8.dp))
   if(active.isNotBlank())Button(onClick={MinecraftButtonMods.disable(c);refresh();message="Мод выключен"},modifier=Modifier.fillMaxWidth()){Text("Выключить активный мод")}
   OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("Поиск кастомизаций")})
   Spacer(Modifier.height(8.dp))
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){
    Button(onClick={create=true},modifier=Modifier.weight(1f)){Icon(Icons.Rounded.Add,null);Text("Создать")}
    OutlinedButton(onClick={importer.launch(arrayOf("application/json","text/json","*/*"))},modifier=Modifier.weight(1f)){Icon(Icons.Rounded.Download,null);Text("Установить")}
   }
   if(message.isNotBlank())Text(message,color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.bodySmall)
   LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(vertical=10.dp)){
    items(filtered,key={it.id}){m->Card(shape=RoundedCornerShape(18.dp)){Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
     Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
      Box(Modifier.size(44.dp).background(Color(m.color),RoundedCornerShape(if(m.shape=="square")6.dp else 14.dp)),contentAlignment=Alignment.Center){Text(m.symbol.ifBlank{"‹ ›"},color=Color.White,fontWeight=FontWeight.Bold)}
      Column(Modifier.weight(1f)){Text(m.name,fontWeight=FontWeight.SemiBold);Text(m.description,style=MaterialTheme.typography.bodySmall)}
      if(active==m.id)AssistChip(onClick={},label={Text("Активен")})
     }
     Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
      Button(onClick={MinecraftButtonMods.setActive(c,m);refresh();message="Мод включён. В игре появится кнопка выключения."},modifier=Modifier.weight(1f)){Text("Применить")}
      OutlinedButton(onClick={val i=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_SUBJECT,"Кастомизация Verity");putExtra(Intent.EXTRA_TEXT,MinecraftButtonMods.exportJson(m))};c.startActivity(Intent.createChooser(i,"Поделиться набором"))},modifier=Modifier.weight(1f)){Icon(Icons.Rounded.Share,null);Text("Поделиться")}
     }
     if(!m.builtin)TextButton(onClick={MinecraftButtonMods.delete(c,m.id);refresh()},modifier=Modifier.align(Alignment.End)){Text("Удалить")}
    }}}
   }
  }
 }
 if(create){var name by remember{mutableStateOf("Моя кастомизация")};var symbol by remember{mutableStateOf("✦")};var color by remember{mutableIntStateOf(modColors.first().second)};var shape by remember{mutableStateOf("round")}
  AlertDialog(onDismissRequest={create=false},title={Text("Создать кастомизацию")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
   OutlinedTextField(name,{name=it.take(40)},label={Text("Название")},singleLine=true);OutlinedTextField(symbol,{symbol=it.take(4)},label={Text("Значок на кнопках")},singleLine=true);Text("Цвет")
   modColors.chunked(3).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){row.forEach{(label,v)->FilterChip(color==v,{color=v},label={Text(label)})}}}
   Text("Форма");Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("round" to "Круглая","square" to "Угловая","neon" to "Неон").forEach{(k,v)->FilterChip(shape==k,{shape=k},label={Text(v)})}}
  }},confirmButton={Button(onClick={if(name.isNotBlank()){val m=MinecraftButtonMods.add(c,MinecraftButtonMods.Mod("",name,color,shape,symbol,"Создано пользователем"));MinecraftButtonMods.setActive(c,m);refresh();create=false}},enabled=name.isNotBlank()){Text("Сохранить и включить")}},dismissButton={TextButton(onClick={create=false}){Text("Отмена")}})
 }
}
''',encoding="utf-8")

game=JAVA/"service/GameOverlayService.java"
s=game.read_text(encoding="utf-8")
def greplace(old,new,label):
 global s
 n=s.count(old)
 if n!=1: raise SystemExit(f"{label}: expected one match, got {n}")
 s=s.replace(old,new,1)
greplace("import android.graphics.Paint;\n","import android.graphics.Paint;\nimport android.graphics.Path;\n","Path import")
greplace("import dev.veriti.app.R;\n","import dev.veriti.app.R;\nimport dev.veriti.app.MainActivity;\n","MainActivity import")
greplace("private boolean verityGrounded = false, moveLeftHeld = false, moveRightHeld = false, jumpQueued = false;\n","private boolean verityGrounded = false, moveLeftHeld = false, moveRightHeld = false, jumpQueued = false;\nprivate boolean scaryVerity=false; private float scaryWalkPhase=0f,verityFacing=1f; private int scaryAttackFrames=0;\n","scary state")
greplace('    if (!running) initializeWorld();\n','    scaryVerity=getSharedPreferences("veriti_data",MODE_PRIVATE).getBoolean("minecraft_scary_verity",false);\n    if (!running) initializeWorld();\n',"load scary state")
greplace("private void updateWorld() { keepWorldGenerated(); updateVerityPhysics();","private void updateWorld() { keepWorldGenerated(); updateVerityPhysics(); if(Math.abs(verityVelocityX)>0.012f){scaryWalkPhase+=0.20f+Math.abs(verityVelocityX)*3f;verityFacing=verityVelocityX<0?-1f:1f;}else{scaryWalkPhase+=0.025f;} if(scaryAttackFrames>0)scaryAttackFrames--;","animation tick")
old='''private int controlAt(float x,float y) {
    float cy=getHeight()-dp(112), r=dp(29), lx=dp(52), rx=dp(116), jx=getWidth()-dp(58);
    if (Math.hypot(x-lx,y-cy)<=r) return 1;
    if (Math.hypot(x-rx,y-cy)<=r) return 2;
    if (Math.hypot(x-jx,y-cy)<=r) return 3;
    return 0;
}
private void drawControls(Canvas canvas) {
    float cy=getHeight()-dp(112), r=dp(27), lx=dp(52), rx=dp(116), jx=getWidth()-dp(58);
    paint.setColor(0xA5222222); canvas.drawCircle(lx,cy,r,paint); canvas.drawCircle(rx,cy,r,paint); canvas.drawCircle(jx,cy,r,paint);
    paint.setColor(0xFFEFEFEF); paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(dp(24));
    canvas.drawText("‹",lx,cy+dp(8),paint); canvas.drawText("›",rx,cy+dp(8),paint); canvas.drawText("↑",jx,cy+dp(8),paint);
    paint.setTextAlign(Paint.Align.LEFT); paint.setTextSize(dp(10)); paint.setColor(0xDFFFFFFF);
    canvas.drawText("Verity · управление",dp(12),getHeight()-dp(151),paint);
}
private void drawVerity(Canvas canvas) {
    float x=originX+verityX*tile, y=originY+verityY*tile;
    RectF dst=new RectF(x-tile*0.9f,y,x+tile*0.9f,y+tile*1.8f);
    if (verityBitmap!=null && !verityBitmap.isRecycled()) { paint.setAlpha(255); canvas.drawBitmap(verityBitmap,null,dst,paint); }
    else { paint.setColor(Color.YELLOW); canvas.drawOval(dst,paint); paint.setColor(Color.BLACK); canvas.drawCircle(x-tile*0.3f,y+tile*0.7f,tile*0.08f,paint); canvas.drawCircle(x+tile*0.3f,y+tile*0.7f,tile*0.08f,paint); }
}
'''
new='''private int controlAt(float x,float y){
 float cy=getHeight()-dp(112),r=dp(29),lx=dp(52),rx=dp(116),jx=getWidth()-dp(58);
 if(new RectF(getWidth()-dp(148),dp(10),getWidth()-dp(8),dp(44)).contains(x,y))return 4;
 if(new RectF(getWidth()-dp(148),dp(50),getWidth()-dp(8),dp(84)).contains(x,y))return 5;
 if(!getSharedPreferences("veriti_data",MODE_PRIVATE).getString("minecraftButtonModActive","").isEmpty()&&new RectF(dp(8),dp(10),dp(128),dp(44)).contains(x,y))return 6;
 if(Math.hypot(x-lx,y-cy)<=r)return 1;if(Math.hypot(x-rx,y-cy)<=r)return 2;if(Math.hypot(x-jx,y-cy)<=r)return 3;return 0;
}
private void drawControls(Canvas c){
 float cy=getHeight()-dp(112),r=dp(27),lx=dp(52),rx=dp(116),jx=getWidth()-dp(58);
 android.content.SharedPreferences prefs=getSharedPreferences("veriti_data",MODE_PRIVATE);String active=prefs.getString("minecraftButtonModActive","");
 int color=active.isEmpty()?0xA5222222:prefs.getInt("minecraftButtonModColor",0xA5222222);
 String shape=active.isEmpty()?"round":prefs.getString("minecraftButtonModShape","round"),symbol=active.isEmpty()?"":prefs.getString("minecraftButtonModSymbol","");
 styledButton(c,lx,cy,r,color,shape);styledButton(c,rx,cy,r,color,shape);styledButton(c,jx,cy,r,color,shape);
 paint.setColor(0xFFEFEFEF);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(dp(24));c.drawText("‹",lx,cy+dp(8),paint);c.drawText("›",rx,cy+dp(8),paint);c.drawText("↑",jx,cy+dp(8),paint);
 if(!symbol.isEmpty()){paint.setTextSize(dp(9));paint.setAlpha(210);c.drawText(symbol,lx,cy-dp(13),paint);c.drawText(symbol,rx,cy-dp(13),paint);c.drawText(symbol,jx,cy-dp(13),paint);}
 paint.setTextAlign(Paint.Align.LEFT);paint.setTextSize(dp(10));paint.setColor(0xDFFFFFFF);c.drawText("Minecraft 2D · управление",dp(12),getHeight()-dp(151),paint);
 Paint.Style old=paint.getStyle();paint.setStyle(Paint.Style.FILL);paint.setColor(scaryVerity?0xE6A52A22:0xD0444444);
 RectF t=new RectF(getWidth()-dp(148),dp(10),getWidth()-dp(8),dp(44));c.drawRoundRect(t,dp(10),dp(10),paint);paint.setColor(Color.WHITE);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(dp(11));c.drawText(scaryVerity?"Verity: СТРАШНЫЙ":"Verity: ОБЫЧНЫЙ",t.centerX(),t.centerY()+dp(4),paint);
 paint.setColor(0xD05B2424);RectF a=new RectF(getWidth()-dp(148),dp(50),getWidth()-dp(8),dp(84));c.drawRoundRect(a,dp(10),dp(10),paint);paint.setColor(Color.WHITE);c.drawText("АТАКА",a.centerX(),a.centerY()+dp(4),paint);
 if(!active.isEmpty()){paint.setColor(0xE63A713E);RectF d=new RectF(dp(8),dp(10),dp(128),dp(44));c.drawRoundRect(d,dp(10),dp(10),paint);paint.setColor(Color.WHITE);paint.setTextSize(dp(10));c.drawText("ВЫКЛЮЧИТЬ МОД",d.centerX(),d.centerY()+dp(4),paint);}
 paint.setTextAlign(Paint.Align.LEFT);paint.setStyle(old);
}
private void styledButton(Canvas c,float x,float y,float r,int color,String shape){
 paint.setStyle(Paint.Style.FILL);paint.setColor(color);
 if("square".equals(shape))c.drawRoundRect(new RectF(x-r,y-r,x+r,y+r),dp(6),dp(6),paint);else c.drawCircle(x,y,r,paint);
 if("neon".equals(shape)){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));paint.setColor(0xFF75F5FF);if("square".equals(shape))c.drawRoundRect(new RectF(x-r,y-r,x+r,y+r),dp(6),dp(6),paint);else c.drawCircle(x,y,r,paint);paint.setStyle(Paint.Style.FILL);}
}
private void drawVerity(Canvas c){float x=originX+verityX*tile,y=originY+verityY*tile;if(scaryVerity){drawScaryVerity(c,x,y);return;}RectF d=new RectF(x-tile*.9f,y,x+tile*.9f,y+tile*1.8f);if(verityBitmap!=null&&!verityBitmap.isRecycled()){paint.setAlpha(255);c.drawBitmap(verityBitmap,null,d,paint);}else{paint.setColor(Color.YELLOW);c.drawOval(d,paint);}}
private void drawScaryVerity(Canvas c,float x,float y){
 float t=tile,step=(float)Math.sin(scaryWalkPhase)*.20f;Paint p=paint,backup=new Paint(p);c.save();c.translate(x,y);c.scale(verityFacing,1f);
 int gold=Color.rgb(230,190,42),light=Color.rgb(255,224,82),shadow=Color.rgb(151,112,20),dark=Color.rgb(65,49,9);
 p.setAntiAlias(true);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.FILL);p.setColor(gold);c.drawRoundRect(new RectF(-t*.43f,-t*2.42f,t*.40f,-t*1.55f),t*.34f,t*.34f,p);
 p.setColor(light);c.drawOval(new RectF(-t*.28f,-t*2.34f,t*.23f,-t*1.65f),p);p.setColor(gold);c.drawRoundRect(new RectF(-t*.12f,-t*1.62f,t*.12f,-t*1.18f),t*.1f,t*.1f,p);c.drawRoundRect(new RectF(-t*.28f,-t*1.25f,t*.27f,t*.48f),t*.18f,t*.18f,p);
 p.setColor(shadow);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(t*.07f);c.drawLine(-t*.18f,-t*.93f,t*.18f,-t*.55f,p);c.drawLine(-t*.20f,-t*.60f,t*.20f,-t*.23f,p);c.drawLine(-t*.15f,-t*.26f,t*.18f,t*.10f,p);
 p.setColor(gold);p.setStrokeWidth(t*.19f);Path a=new Path();a.moveTo(-t*.23f,-t*1.12f);a.lineTo(-t*.66f,-t*.55f-step*t);a.lineTo(-t*.76f,t*.46f-step*t);a.lineTo(-t*.82f,t*1.25f-step*t);c.drawPath(a,p);
 Path b=new Path();b.moveTo(t*.22f,-t*1.12f);b.lineTo(t*.58f,-t*.45f+step*t);b.lineTo(t*.80f,t*.50f+step*t);b.lineTo(t*.88f,t*1.25f+step*t);c.drawPath(b,p);
 p.setStrokeWidth(t*.22f);Path l=new Path();l.moveTo(-t*.12f,t*.62f);l.lineTo(-t*.32f,t*1.05f+step*t);l.lineTo(-t*.47f,t*1.67f+step*t);c.drawPath(l,p);Path r=new Path();r.moveTo(t*.12f,t*.62f);r.lineTo(t*.33f,t*1.06f-step*t);r.lineTo(t*.48f,t*1.67f-step*t);c.drawPath(r,p);
 p.setStrokeWidth(t*.16f);c.drawLine(-t*.47f,t*1.67f+step*t,-t*.78f,t*1.73f+step*t,p);c.drawLine(t*.48f,t*1.67f-step*t,t*.79f,t*1.73f-step*t,p);
 p.setStyle(Paint.Style.FILL);p.setColor(dark);c.drawOval(new RectF(t*.08f,-t*2.14f,t*.31f,-t*1.86f),p);p.setColor(Color.BLACK);c.drawRoundRect(new RectF(-t*.30f,-t*1.78f,t*.23f,-t*1.57f),t*.08f,t*.08f,p);p.setColor(Color.WHITE);for(int i=0;i<4;i++)c.drawRect(-t*.25f+i*t*.11f,-t*1.75f,-t*.19f+i*t*.11f,-t*1.66f,p);
 if(scaryAttackFrames>0){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(t*.23f);p.setColor(gold);c.drawLine(t*.22f,-t*1.12f,t*1.22f,-t*.92f,p);c.drawLine(t*1.22f,-t*.92f,t*1.67f,-t*.83f,p);p.setStrokeWidth(t*.09f);c.drawLine(t*1.62f,-t*.82f,t*1.86f,-t*.96f,p);c.drawLine(t*1.62f,-t*.82f,t*1.88f,-t*.78f,p);}
 c.restore();p.set(backup);
}
private void openVerityChat(){try{Intent i=new Intent(GameOverlayService.this,MainActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);startActivity(i);}catch(Exception e){Toast.makeText(GameOverlayService.this,"Не удалось открыть чат Verity",Toast.LENGTH_SHORT).show();}}
'''
greplace(old,new,"Minecraft overlay render")
greplace('''if (control!=0) {
            controlTouch=true; activeControl=control; moveLeftHeld=control==1; moveRightHeld=control==2;
            if (control==3) jumpQueued=true;
            invalidate(); return true;
        }''','''if(control!=0){
 if(control==4){scaryVerity=!scaryVerity;getSharedPreferences("veriti_data",MODE_PRIVATE).edit().putBoolean("minecraft_scary_verity",scaryVerity).apply();invalidate();return true;}
 if(control==5){scaryAttackFrames=16;invalidate();return true;}
 if(control==6){getSharedPreferences("veriti_data",MODE_PRIVATE).edit().putString("minecraftButtonModActive","").remove("minecraftButtonModColor").remove("minecraftButtonModShape").remove("minecraftButtonModSymbol").remove("minecraftButtonModName").apply();invalidate();return true;}
 controlTouch=true;activeControl=control;moveLeftHeld=control==1;moveRightHeld=control==2;if(control==3)jumpQueued=true;invalidate();return true;
}''',"touch controls")
greplace('''int x = (int) Math.floor((ux - originX) / tile); int y = (int) Math.floor((uy - originY) / tile); if (inside(x, y)) {''','''float px=(ux-originX)/tile,py=(uy-originY)/tile;
 if(Math.abs(px-verityX)<=(scaryVerity?1.55f:1f)&&py>=verityY-(scaryVerity?2.55f:.15f)&&py<=verityY+1.9f){if(scaryVerity)scaryAttackFrames=14;openVerityChat();invalidate();return true;}
 int x = (int) Math.floor((ux - originX) / tile); int y = (int) Math.floor((uy - originY) / tile); if (inside(x, y)) {''',"tap Verity")
game.write_text(s,encoding="utf-8")
print("Verity custom updates applied successfully")


# ---------------------------------------------------------------------------
# Requested Verity skin additions (exact user-supplied image cut-outs)
# ---------------------------------------------------------------------------
import shutil

skin_atlas_source = Path(__file__).with_name("verity_new_skins_atlas.png")
skin_atlas_target = ROOT / "app/src/main/assets/character_catalog/verity_new_skins_atlas.png"
if not skin_atlas_source.is_file():
    raise SystemExit(f"Requested skin atlas not found: {skin_atlas_source}")
skin_atlas_target.parent.mkdir(parents=True, exist_ok=True)
shutil.copy2(skin_atlas_source, skin_atlas_target)

catalog = JAVA / "CharacterCatalogScreen.kt"
rep(catalog,
    'import dev.veriti.app.data.CustomCharacterLibrary\n',
    'import dev.veriti.app.data.CustomCharacterLibrary\nimport dev.veriti.app.data.SkinSheet\n',
    "import exact-image skin loader")
rep(catalog,
    '"Красный сердитый персонаж", "Светлый розовый смайлик"\n)',
    '"Красный сердитый персонаж", "Светлый розовый смайлик",\n'
    '    "Первый прототип Verity", "Коробка-посылка", "Трёхглазый смайлик",\n'
    '    "Телевизор с антеннами", "Скелетик", "Тыквенная голова", "Кратерная улыбка Гейла"\n)',
    "append six exact-image skin groups")
rep(catalog,
    '"Сердитик", "Розик"\n)',
    '"Сердитик", "Розик", "Прототип", "Посылыч", "Трио", "ТВик", "Костик", "Тыквик", "Гейл"\n)',
    "append skin nicknames")
rep(catalog,
'''        } else {
            runCatching { context.assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
        }
''',
'''        } else if (path.startsWith("character_catalog/sprites/49/") ||
            path.startsWith("character_catalog/sprites/50/") ||
            path.startsWith("character_catalog/sprites/51/") ||
            path.startsWith("character_catalog/sprites/52/") ||
            path.startsWith("character_catalog/sprites/53/") ||
            path.startsWith("character_catalog/sprites/54/") ||
            path.startsWith("character_catalog/sprites/55/")) {
            runCatching { SkinSheet.loadCatalogSprite(context, path)?.asImageBitmap() }.getOrNull()
        } else {
            runCatching { context.assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
        }
''',
    "display exact-image skins in catalog")
rep(catalog, "(1..48).filter {", "(1..55).filter {", "search seven new skin groups")
rep(catalog, "Встроенные 48 групп нельзя удалить.", "Встроенные 55 групп нельзя удалить.", "catalog group count")
rep(catalog,
'''                            Text("12 отдельных изображений · выбрать", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
''',
'''                            val info = when (id) {
                                49 -> "8 выражений · первый прототип Verity"
                                55 -> "Реальное фото кратера Гейла · круглый вырез"
                                else -> "Оригинальный статичный образ"
                            }
                            Text(info, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
''',
    "new skin card descriptions")
rep(catalog, "Встроенные 48 групп персонажей удалить нельзя.", "Встроенные 55 групп персонажей удалить нельзя.", "custom character delete text")
rep(catalog,
'''    if (selectedGroup in 1..48) {
''',
'''    if (selectedGroup in 1..55) {
''',
    "open all 54 skin groups")
rep(catalog,
'''                    Text("В каждом наборе 12 вырезанных кадров. Персонаж использует кадры для 12 настроений (радость, нейтральное, злость, грусть, речь, испуг, любопытство, сонливость, смех, удивление, симпатия и переходные состояния). В исходной таблице эмоции не подписаны, поэтому соответствие приблизительное.", style = MaterialTheme.typography.bodySmall)
''',
'''                    Text(
                        when (selectedGroup) {
                            49 -> "Восемь исходных эмоций вырезаны отдельно из присланной таблицы и используются как разные выражения персонажа."
                            in 50..55 -> if (selectedGroup == 55) "Круглая фотография кратера Гейла на Марсе из архива NASA." else "Оригинальный персонаж с прозрачным фоном."
                            else -> "В каждом исходном наборе 12 вырезанных кадров."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
''',
    "explain exact-image skin frames")
rep(catalog,
'''                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxWidth().height(248.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items((1..12).toList()) { variant ->
''',
'''                    val frameCount = when (selectedGroup) {
                        49 -> 8
                        in 50..55 -> 1
                        else -> 12
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxWidth().height(if (frameCount <= 4) 105.dp else 248.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items((1..frameCount).toList()) { variant ->
''',
    "show only real emotion frames")
rep(catalog,
'''                    }) { Text("Использовать набор эмоций") }
''',
'''                    }) { Text(if (selectedGroup == 49) "Использовать набор эмоций" else "Использовать персонажа") }
''',
    "static skin selection label")

lore = DATA / "CharacterLore.kt"
rep(lore,
'''        Entry(48, "Розик", "Розик — мягкий и приветливый спутник. Он любит рисовать сердечки, поддерживать друзей и устраивать тихие праздники.")
''',
'''        Entry(48, "Розик", "Розик — мягкий и приветливый спутник. Он любит рисовать сердечки, поддерживать друзей и устраивать тихие праздники."),
        Entry(49, "Первый прототип Verity", "Восемь эмоций из самой первой таблицы смайликов — первоначальный визуальный прототип Verity."),
        Entry(50, "Посылыч", "Посылыч — небольшая коробка-посылка с запиской. Он любит доставлять добрые идеи и хранить маленькие сюрпризы."),
        Entry(51, "Трио", "Трио — смайлик с тремя глазами. Он любит наблюдать за деталями и сначала смотреть, а потом делать выводы."),
        Entry(52, "ТВик", "ТВик — ретро-телевизор с антеннами. Он хранит смешные передачи и короткие загадки."),
        Entry(53, "Костик", "Костик — оригинальный дружелюбный скелетик, нарисованный специально для каталога Verity."),
        Entry(54, "Тыквик", "Тыквик — оригинальный персонаж-тыква со светящейся улыбкой, нарисованный для каталога Verity."),
        Entry(55, "Гейл", "Гейл — круглый снимок кратера Гейла на Марсе, известного как Happy Face Crater. Улыбку создаёт естественный рельеф и иней на поверхности.")
''',
    "add six character mini-stories")
rep(lore, "takeIf { it in 1..48 }", "takeIf { it in 1..55 }", "recognise seven new character assets")

skin = DATA / "SkinSheet.kt"
new_skin_helper = r'''    private fun loadVerityNewSkin(context: Context, group: Int, variant: Int?, mood: String): Bitmap? {
        // Atlas cells are exact, transparent crops from the supplied reference images.
        // Index order: 8 smiley emotions, parcel, 3-eye face, TV, skeleton, pumpkin.
        val slot = when (group) {
            49 -> {
                val direct = variant?.minus(1)?.takeIf { it in 0..7 }
                direct ?: when (mood) {
                    "happy", "love" -> 0
                    "talking" -> 1
                    "normal", "curious" -> 2
                    "laughing" -> 3
                    "sad", "sleepy" -> 4
                    "ouch", "surprised" -> 5
                    "scary" -> 6
                    "angry" -> 7
                    else -> 0
                }
            }
            50 -> 8
            51 -> 9
            52 -> 10
            53 -> 11
            54 -> 12
            55 -> 13
            else -> return null
        }
        val atlas = runCatching {
            context.assets.open("character_catalog/verity_new_skins_atlas.png").use(BitmapFactory::decodeStream)
        }.getOrNull() ?: return null
        val cell = minOf(atlas.width / 4, atlas.height / 4)
        if (cell < 1 || atlas.width < cell * 4 || atlas.height < cell * 4) {
            atlas.recycle()
            return null
        }
        val crop = runCatching {
            Bitmap.createBitmap(atlas, (slot % 4) * cell, (slot / 4) * cell, cell, cell)
                .copy(Bitmap.Config.ARGB_8888, true)
        }.getOrNull()
        atlas.recycle()
        // Do not remove dark pixels: the source art's outlines and TV screen are black.
        return crop
    }

'''
rep(skin,
'''    fun loadCatalogSprite(context: Context, assetPath: String, mood: String = "happy"): Bitmap? {
''',
new_skin_helper + '''    fun loadCatalogSprite(context: Context, assetPath: String, mood: String = "happy"): Bitmap? {
''',
    "add exact image atlas cropper")
rep(skin,
'''        if (!assetPath.startsWith("character_catalog/sprites/") || assetPath.contains("..")) return null
        val resolvedPath = if (assetPath.endsWith("/")) {
''',
'''        if (!assetPath.startsWith("character_catalog/sprites/") || assetPath.contains("..")) return null
        val addedMatch = Regex("character_catalog/sprites/(\\\\d{2})(?:/|_)").find(assetPath)
        val addedGroup = addedMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (addedGroup != null && addedGroup in 49..55) {
            val variant = Regex("_(\\\\d{2})\\\\.png$").find(assetPath)?.groupValues?.getOrNull(1)?.toIntOrNull()
            return loadVerityNewSkin(context, addedGroup, variant, mood)
        }
        val resolvedPath = if (assetPath.endsWith("/")) {
''',
    "route exact-image atlas skins")

# Keep the catalog counts truthful everywhere they are displayed.
for ui_file in (JAVA / "MainActivity.kt", JAVA / "FeatureListScreen.kt"):
    ui = ui_file.read_text(encoding="utf-8")
    ui = ui.replace("каталог из 48 групп", "каталог из 55 групп")
    ui = ui.replace("48 групп из твоей таблицы", "55 встроенных групп, включая фото кратера NASA")
    ui = ui.replace("48 групп / 576 кадров и 48 предысторий", "55 групп / 590 исходных изображений")
    ui = ui.replace("48 групп разделены на 576 PNG-кадров",
                    "48 исходных групп имеют по 12 кадров; добавлены 8 эмоций, коробка, трёхглазый смайлик, телевизор, скелетик и тыквенная голова")
    ui = ui.replace("встроенные 48 групп нельзя удалить", "встроенные 55 групп нельзя удалить")
    ui_file.write_text(ui, encoding="utf-8")

print("Added seven Verity skin groups (49–55), including NASA Galle Crater")

# Source atlas is a transparent PNG; keep its binary payload intact in the APK.
