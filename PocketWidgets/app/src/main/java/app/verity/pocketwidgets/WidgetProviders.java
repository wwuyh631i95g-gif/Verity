package app.verity.pocketwidgets;
import android.app.PendingIntent;import android.appwidget.*;import android.content.*;import android.net.Uri;import android.os.SystemClock;import android.provider.Settings;import android.view.View;import android.widget.RemoteViews;import java.util.*;
public final class WidgetProviders {
 public abstract static class Base extends AppWidgetProvider { protected abstract String type(); @Override public final void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)Views.update(c,m,type(),id);} }
 public static final class QuickSettings extends Base{protected String type(){return "quick";}}
 public static final class NewYear extends Base{protected String type(){return "year";}}
 public static final class TiltSand extends Base{protected String type(){return "sand";}}
 public static final class DinoRunner extends Base{protected String type(){return "dino";}}
 public static final class RoboVac extends Base{protected String type(){return "vac";}}
 public static final class PopIt extends Base{protected String type(){return "pop";}}
 public static final class Excuse extends Base{protected String type(){return "excuse";}}
 public static final class Aquarium extends Base{protected String type(){return "fish";}}
 public static final class Robot extends Base{protected String type(){return "robot";}}
 public static final class Glass extends Base{protected String type(){return "glass";}}
 public static final class PetTimer extends Base{protected String type(){return "pet";}}
 public static final class Workday extends Base{protected String type(){return "work";}}
 public static final class FlipBottle extends Base{protected String type(){return "flip";}}
 public static final class SixSeven extends Base{protected String type(){return "sixseven";}}
 public static final class Memory extends Base{protected String type(){return "memory";}}

 public static final class ActionReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context c,Intent i){
   String t=i.getStringExtra("type");int id=i.getIntExtra("id",-1),a=i.getIntExtra("a",0);if(t==null||id<0)return;
   if(t.equals("quick")){String s=a==1?Settings.ACTION_WIFI_SETTINGS:a==2?Settings.ACTION_BLUETOOTH_SETTINGS:a==3?Settings.ACTION_DISPLAY_SETTINGS:Settings.ACTION_SOUND_SETTINGS;try{c.startActivity(new Intent(s).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Exception ignored){}return;}
   String k=id+"_";android.content.SharedPreferences p=c.getSharedPreferences("pw",0);android.content.SharedPreferences.Editor e=p.edit();
   switch(t){
    case "sand":e.putInt(k+"tilt",a==1?-1:a==3?1:0);break;
    case "dino":if(a==1){int s=p.getInt(k+"score",0)+1;e.putInt(k+"score",s).putInt(k+"best",Math.max(s,p.getInt(k+"best",0)));}else e.putInt(k+"score",0);break;
    case "vac":{int pos=p.getInt(k+"pos",4),mask=p.getInt(k+"dirt",511);if(a==1)pos=Math.max(0,pos-1);else if(a==3)pos=Math.min(8,pos+1);else if(a==2)mask&=~(1<<pos);else if(a>=50&&a<59)pos=a-50;if(mask==0)mask=511;e.putInt(k+"pos",pos).putInt(k+"dirt",mask);break;}
    case "pop":if(a==2)e.putInt(k+"mask",0);else if(a>=20&&a<26)e.putInt(k+"mask",p.getInt(k+"mask",0)^(1<<(a-20)));break;
    case "excuse":e.putInt(k+"n",p.getInt(k+"n",0)+1);break;
    case "fish":e.putInt(k+"feed",p.getInt(k+"feed",0)+1);break;
    case "robot":e.putInt(k+"mood",p.getInt(k+"mood",0)+1);break;
    case "glass":e.putInt(k+"cracks",a==2?0:Math.min(4,p.getInt(k+"cracks",0)+1));break;
    case "pet":e.putLong(k+"last",System.currentTimeMillis()).putInt(k+"happy",1-p.getInt(k+"happy",1));break;
    case "work":e.putLong(k+"end",p.getLong(k+"end",nextEnd())+(a==1?-1800000L:1800000L));break;
    case "flip":{int s=p.getInt(k+"streak",0)+(new Random().nextBoolean()?1:0);e.putInt(k+"streak",s).putInt(k+"turn",p.getInt(k+"turn",0)+1).putInt(k+"best",Math.max(s,p.getInt(k+"best",0)));break;}
    case "sixseven":{int target=p.getInt(k+"target",6);if(a==8)e.putInt(k+"target",new Random().nextBoolean()?6:7);else if(a==target)e.putInt(k+"score",p.getInt(k+"score",0)+1).putInt(k+"target",new Random().nextBoolean()?6:7);else e.putInt(k+"score",0);break;}
    case "memory":{if(a==2)e.putString(k+"board",shuffle()).putInt(k+"matched",0).putInt(k+"one",-1).putInt(k+"two",-1);else if(a>=100&&a<116){int n=a-100,mask=p.getInt(k+"matched",0),one=p.getInt(k+"one",-1),two=p.getInt(k+"two",-1);if((mask&(1<<n))==0){if(one>=0&&two>=0){one=-1;two=-1;}if(one<0)one=n;else if(one!=n){two=n;String b=p.getString(k+"board","●◆▲■✦✚★♥●◆▲■✦✚★♥");if(b.substring(one,one+1).equals(b.substring(two,two+1))){mask|=(1<<one)|(1<<two);one=-1;two=-1;}}e.putInt(k+"matched",mask).putInt(k+"one",one).putInt(k+"two",two);}}break;}
   }
   e.apply();Views.updateOne(c,t,id);
  }
  static long nextEnd(){Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,18);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);if(c.getTimeInMillis()<=System.currentTimeMillis())c.add(Calendar.DAY_OF_YEAR,1);return c.getTimeInMillis();}
  static String shuffle(){char[] a="●◆▲■✦✚★♥●◆▲■✦✚★♥".toCharArray();Random r=new Random();for(int n=a.length-1;n>0;n--){int j=r.nextInt(n+1);char z=a[n];a[n]=a[j];a[j]=z;}return new String(a);}
 }
 static final class Views {
  static int v(Context c,int id,String key,int def){return c.getSharedPreferences("pw",0).getInt(id+"_"+key,def);}
  static long l(Context c,int id,String key,long def){return c.getSharedPreferences("pw",0).getLong(id+"_"+key,def);}
  static void updateOne(Context c,String t,int id){update(c,AppWidgetManager.getInstance(c),t,id);}
  static void update(Context c,AppWidgetManager m,String t,int id){
   RemoteViews r=new RemoteViews(c.getPackageName(),R.layout.widget_home);
   String title="",main="",sub="",s1="",s2="",s3="",s4="";int a1=-1,a2=-1,a3=-1,a4=-1,count=0;boolean clock=false,down=false;long base=0;
   switch(t){
    case "quick":title="QUICK SETTINGS";main="QUICK ACCESS";sub="System shortcuts";s1="Wi-Fi";s2="Bluetooth";s3="Display";s4="Sound";a1=1;a2=2;a3=3;a4=4;break;
    case "year":{title="NEW YEAR";sub="Time remaining until January 1";Calendar n=Calendar.getInstance(),e=Calendar.getInstance();e.set(Calendar.MONTH,0);e.set(Calendar.DAY_OF_MONTH,1);e.set(Calendar.HOUR_OF_DAY,0);e.set(Calendar.MINUTE,0);e.set(Calendar.SECOND,0);if(n.getTimeInMillis()>=e.getTimeInMillis())e.add(Calendar.YEAR,1);long d=e.getTimeInMillis()-n.getTimeInMillis();main=(d/86400000L)+" DAYS";base=SystemClock.elapsedRealtime()+Math.max(1000,d%86400000L);clock=true;down=true;s1="FIREWORKS";a1=1;break;}
    case "sand":{title="TILT SAND";sub="Move the falling sand";int x=v(c,id,"tilt",0);main=x<0?"◀ ░░●░░":x>0?"░░●░░ ▶":"░░░●░░░";s1="◀";s2="SHAKE";s3="▶";a1=1;a2=2;a3=3;break;}
    case "dino":title="DINO RUNNER";sub="Jump over cactus";main="🦖 "+v(c,id,"score",0)+"  ·  BEST "+v(c,id,"best",0);s1="JUMP ▲";s2="RESET";a1=1;a2=2;break;
    case "vac":{title="ROBOT VACUUM";sub="Move and collect the fluff";count=9;int pos=v(c,id,"pos",4),mask=v(c,id,"dirt",511);for(int x=0;x<9;x++)cell(c,r,id,t,x,x==pos?"🤖":(mask&(1<<x))!=0?"✦":"·",50+x);s1="◀";s2="CLEAN";s3="▶";a1=1;a2=2;a3=3;break;}
    case "pop":{title="POP-IT";sub="Tap the bubbles";count=6;int mask=v(c,id,"mask",0);for(int x=0;x<6;x++)cell(c,r,id,t,x,(mask&(1<<x))!=0?"●":"○",20+x);main=Integer.bitCount(mask)+"/6 POPPED";s1="RESET";a1=2;break;}
    case "excuse":{title="WHY AM I LATE?";sub="Get another phrase";String[] p={"Я перепутал дни недели","Автобус уехал раньше","Искал пропавший ключ","Помогал соседу","Телефон обновлялся","Застрял в лифте","Не сработал будильник"};main="«"+p[Math.floorMod(v(c,id,"n",0),p.length)]+"»";s1="NEXT PHRASE";a1=1;break;}
    case "fish":{title="AQUARIUM";sub="Feed the fish";int f=v(c,id,"feed",0);main=(f%2==0?"🫧 🐟 🫧":"🐠 🫧 🐟")+"\nFEED "+f;s1="FEED FISH";a1=1;break;}
    case "robot":{title="REACTIVE ROBOT";sub="Touch for a new expression";String[] f={"( •ᴗ• )","( ◕‿◕ )","( ಠ_ಠ )","( ◉o◉ )","( ˘ω˘ )"},p={"HELLO!","I LIKE THIS","I'M THINKING","WOW!","Zzz..."};int x=v(c,id,"mood",0)%5;main=f[x]+"\n"+p[x];s1="TOUCH ROBOT";a1=1;break;}
    case "glass":{title="BREAK THE GLASS";sub="Tap to crack, reset to repair";int x=Math.min(4,v(c,id,"cracks",0));String[] g={"◇ ◇ ◇","◇ ╱◇ ◇","◇ ╱╲◇ ╱","╱╲╳╱╲","╳╳╳╳"};main=g[x]+"\n"+(x==0?"UNBROKEN":x==4?"BROKEN":"CRACKS "+x);s1="TAP GLASS";s2="RESET";a1=1;a2=2;break;}
    case "pet":{title="DON'T FORGET ME";sub="Touch to reset the pet timer";main=v(c,id,"happy",1)==1?"(｡•ᴗ•｡)":"(｡•́︿•̀｡)";s1="PET ME";a1=1;long last=l(c,id,"last",System.currentTimeMillis());base=SystemClock.elapsedRealtime()-Math.max(0,System.currentTimeMillis()-last);clock=true;break;}
    case "work":{title="WORKDAY";sub="Adjust finish time by 30 minutes";main="UNTIL FREEDOM";long end=l(c,id,"end",ActionReceiver.nextEnd());if(end<System.currentTimeMillis())end=ActionReceiver.nextEnd();base=SystemClock.elapsedRealtime()+Math.max(1000,end-System.currentTimeMillis());clock=true;down=true;s1="− 30 MIN";s2="+ 30 MIN";a1=1;a2=2;break;}
    case "flip":title="FLIP BOTTLE CHALLENGE";sub="Flip and build your streak";main=(v(c,id,"turn",0)%2==0?"▯":"▱")+"\nSTREAK "+v(c,id,"streak",0)+"  BEST "+v(c,id,"best",0);s1="FLIP BOTTLE";a1=1;break;
    case "sixseven":title="CATCH 6–7";sub="Tap the changing target";main="CATCH "+v(c,id,"target",6)+"!\nSTREAK "+v(c,id,"score",0);s1="6";s2="7";s3="NEW TARGET";a1=6;a2=7;a3=8;break;
    case "memory":{title="MEMORY PAIRS";sub="Find identical symbols";count=16;int mask=v(c,id,"matched",0),o=v(c,id,"one",-1),q=v(c,id,"two",-1);String b=c.getSharedPreferences("pw",0).getString(id+"_board","●◆▲■✦✚★♥●◆▲■✦✚★♥");main=(Integer.bitCount(mask)/2)+"/8 PAIRS";for(int x=0;x<16;x++){boolean show=(mask&(1<<x))!=0||x==o||x==q;cell(c,r,id,t,x,show?b.substring(x,x+1):"·",100+x);}s1="NEW GAME";a1=2;break;}
   default:title="POCKET WIDGET";main="READY";
   }
   r.setTextViewText(R.id.title,title);r.setTextViewText(R.id.main,main);r.setTextViewText(R.id.sub,sub);r.setViewVisibility(R.id.grid,count>0?View.VISIBLE:View.GONE);
   for(int x=0;x<16;x++)r.setViewVisibility(c.getResources().getIdentifier("c"+x,"id",c.getPackageName()),x<count?View.VISIBLE:View.GONE);
   if(clock){r.setViewVisibility(R.id.chrono,View.VISIBLE);r.setChronometer(R.id.chrono,base,"%s",true);if(android.os.Build.VERSION.SDK_INT>=24)r.setChronometerCountDown(R.id.chrono,down);}else r.setViewVisibility(R.id.chrono,View.GONE);
   button(c,r,id,t,R.id.b1,a1,s1);button(c,r,id,t,R.id.b2,a2,s2);button(c,r,id,t,R.id.b3,a3,s3);button(c,r,id,t,R.id.b4,a4,s4);m.updateAppWidget(id,r);
  }
  static void cell(Context c,RemoteViews r,int id,String t,int n,String text,int action){int view=c.getResources().getIdentifier("c"+n,"id",c.getPackageName());r.setTextViewText(view,text);button(c,r,id,t,view,action,"");}
  static void button(Context c,RemoteViews r,int id,String t,int view,int a,String text){if(a<0){r.setViewVisibility(view,View.GONE);return;}r.setViewVisibility(view,View.VISIBLE);if(!text.isEmpty())r.setTextViewText(view,text);Intent i=new Intent(c,ActionReceiver.class).setAction("PW").setData(Uri.parse("pw://"+t+"/"+id+"/"+a)).putExtra("type",t).putExtra("id",id).putExtra("a",a);PendingIntent p=PendingIntent.getBroadcast(c,id*100+a+1,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);r.setOnClickPendingIntent(view,p);}
 }
}