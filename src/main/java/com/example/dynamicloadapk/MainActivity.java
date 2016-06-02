package com.example.dynamicloadapk;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dalvik.system.DexClassLoader;


public class MainActivity extends ActionBarActivity  implements OnClickListener {

    private static final String TAG = "MainActivity";

    private String apkDir = Environment.getExternalStorageDirectory().getPath()+File.separator+"Download";
    private List<HashMap<String,String>> datas;
    private ListView mListView;
    private final List<String> apkName = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //第一步把apk拷贝至sd卡的plugin目录下
        copyApkFile("demo.apk");
        Toast.makeText(this, "拷贝完成", Toast.LENGTH_SHORT).show();

        findViewById(R.id.btn_changebackgound).setOnClickListener(this);
        findViewById(R.id.btn_getvalue_from_plugin_method_nop).setOnClickListener(this);
        findViewById(R.id.btn_getvalue_from_plugin_method_hasp).setOnClickListener(this);
        findViewById(R.id.btn_getvalue_from_plugin_private_method_nop).setOnClickListener(this);
    }

    //拷贝apk文件至sd卡plugin目录下
    private void copyApkFile(String apkName) {
        File file = new File(apkDir);
        if (!file.exists()) {
            file.mkdirs();
        }
        File apk = new File(apkDir + File.separator + apkName);
        try {
            if(apk.exists()){
                return;
            }
            FileOutputStream fos = new FileOutputStream(apk);
            InputStream is = getResources().getAssets().open(apkName);
            BufferedInputStream bis = new BufferedInputStream(is);
            int len = -1;
            byte[] by = new byte[1024];
            while ((len = bis.read(by)) != -1) {
                fos.write(by, 0, len);
                fos.flush();
            }
            fos.close();
            is.close();
            bis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 加载apk获得内部资源
     * (anim, attr,bool,color,dimen,drawable,id,integer,layout,mipmap,string,style,styleable)
     *
     *
     * @param apkDir apk目录
     * @param apkName apk名字,带.apk
     * @throws Exception
     */
    private void dynamicLoadApk(String apkDir, String apkName, String apkPackageName) throws Exception {
        File optimizedDirectoryFile = getDir("dex", Context.MODE_PRIVATE);//在应用安装目录下创建一个名为app_dex文件夹目录,如果已经存在则不创建
        Log.v("zxy", optimizedDirectoryFile.getPath().toString());// /data/data/com.example.dynamicloadapk/app_dex
        //参数：
        // 1、包含dex的apk文件或jar文件的路径;
        // 2、apk、jar解压缩生成dex存储的目录;
        // 3、本地library库目录，一般为null;
        // 4、父ClassLoader;
        DexClassLoader dexClassLoader = new DexClassLoader(apkDir+File.separator+apkName, optimizedDirectoryFile.getPath(), null, ClassLoader.getSystemClassLoader());
        Class<?> clazz = dexClassLoader.loadClass(apkPackageName + ".R$mipmap");//通过使用apk自己的类加载器，反射出R类中相应的内部类进而获取我们需要的资源id
        Log.i(TAG, clazz.getName());
        Field field = clazz.getDeclaredField("one");//得到名为one的这张图片字段
        int resId = field.getInt(R.id.class);//得到图片id
        Resources mResources = getPluginResources(apkName);//得到插件apk中的Resource
        if (mResources != null) {
            //通过插件apk中的Resource得到resId对应的资源
            findViewById(R.id.background).setBackgroundDrawable(mResources.getDrawable(resId));
        }
    }

    /**
     * 加载apk获得内部class的方法
     * @param apkDir apk目录
     * @param apkName apk名字,带.apk
     * @throws Exception
     */
    private void dynamicLoadApkMethodNoParams(String apkDir, String apkName, String apkPackageName) throws Exception {
        File optimizedDirectoryFile = getDir("dex", Context.MODE_PRIVATE);//在应用安装目录下创建一个名为app_dex文件夹目录,如果已经存在则不创建
        Log.v(TAG, optimizedDirectoryFile.getPath().toString());// /data/data/com.example.dynamicloadapk/app_dex

        //参数：
        // 1、包含dex的apk文件或jar文件的路径;
        // 2、apk、jar解压缩生成dex存储的目录;
        // 3、本地library库目录，一般为null;
        // 4、父ClassLoader;
        DexClassLoader dexClassLoader = new DexClassLoader(apkDir+File.separator+apkName, optimizedDirectoryFile.getPath(), null, ClassLoader.getSystemClassLoader());

        Class<?> methClazz = dexClassLoader.loadClass(apkPackageName + ".MainActivity");
        Log.d(TAG, methClazz.getName());
        Method[] mds = methClazz.getMethods();
        Log.d(TAG, "mds.count="+mds.length);
//        for(Method m:mds){
//            Log.d(TAG,"m.getName()="+m.getName());
//        }

        //the method must be public method in com.xxx.MainActivity, the method return int values;
        String methodName = "getBadgeCount";
        Method md = methClazz.getMethod(methodName, new Class[0]);
        md.setAccessible(true);
        Object object = methClazz.newInstance();
        int bc = (int) md.invoke(object, new Object[0]);
        Log.i(TAG, "bc==="+bc);
        Toast.makeText(this, "get BadgeCounts value(none params)="+bc, Toast.LENGTH_SHORT).show();
    }

    /**
     * 加载apk获得内部class的方法
     * @param apkDir apk目录
     * @param apkName apk名字,带.apk
     * @throws Exception
     */
    private void dynamicLoadApkMethodWithParams(String apkDir, String apkName, String apkPackageName) throws Exception {
        File optimizedDirectoryFile = getDir("dex", Context.MODE_PRIVATE);//在应用安装目录下创建一个名为app_dex文件夹目录,如果已经存在则不创建
        Log.v(TAG, optimizedDirectoryFile.getPath().toString());// /data/data/com.example.dynamicloadapk/app_dex
        //参数：
        // 1、包含dex的apk文件或jar文件的路径;
        // 2、apk、jar解压缩生成dex存储的目录;
        // 3、本地library库目录，一般为null;
        // 4、父ClassLoader;
        DexClassLoader dexClassLoader = new DexClassLoader(apkDir+File.separator+apkName, optimizedDirectoryFile.getPath(), null, ClassLoader.getSystemClassLoader());

        Class<?> methClazz = dexClassLoader.loadClass(apkPackageName + ".MainActivity");
        Log.d(TAG, methClazz.getName());
        Method[] mds = methClazz.getMethods();
        Log.d(TAG, "mds.count="+mds.length);
//        for(Method m:mds){
//            Log.d(TAG,"m.getName()="+m.getName());
//        }

        //the method must be public method in com.xxx.MainActivity, the method return int values;
        String methodName = "getBadgeCount";

        //params type is : int
        Method md = methClazz.getMethod(methodName, int.class);
        md.setAccessible(true);

        Object object = methClazz.newInstance();
        int bc = (int) md.invoke(object, 1001);
        Log.i(TAG, "bc==="+bc);
        Toast.makeText(this, "get BadgeCounts value(with Int params)="+bc, Toast.LENGTH_SHORT).show();
    }

    /**
     * 加载apk获得内部class的私有方法
     * @param apkDir apk目录
     * @param apkName apk名字,带.apk
     * @throws Exception
     */
    private void dynamicLoadApkPrivateMethodNoParams(String apkDir, String apkName, String apkPackageName) throws Exception{
        File optimizedDirectoryFile = getDir("dex", Context.MODE_PRIVATE);//在应用安装目录下创建一个名为app_dex文件夹目录,如果已经存在则不创建
        Log.v(TAG, optimizedDirectoryFile.getPath().toString());// /data/data/com.example.dynamicloadapk/app_dex
        //参数：
        // 1、包含dex的apk文件或jar文件的路径;
        // 2、apk、jar解压缩生成dex存储的目录;
        // 3、本地library库目录，一般为null;
        // 4、父ClassLoader;
        DexClassLoader dexClassLoader = new DexClassLoader(apkDir+File.separator+apkName, optimizedDirectoryFile.getPath(), null, ClassLoader.getSystemClassLoader());

        Class<?> methClazz = dexClassLoader.loadClass(apkPackageName + ".MainActivity");

        Constructor constructor = methClazz.getConstructor(new Class[]{});
        Object instance = constructor.newInstance(new Object[]{});
        Method getMoney = methClazz.getDeclaredMethod("getInteger", new Class[0]);
        getMoney.setAccessible(true);
        Object privateValue = getMoney.invoke(instance, new Object[0]);
        Toast.makeText(this, privateValue.toString(), Toast.LENGTH_LONG).show();
    }

    /**
     * @param apkName
     * @return 得到对应插件的Resource对象
     */
    private Resources getPluginResources(String apkName) {
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);//反射调用方法addAssetPath(String path)
            //第二个参数是apk的路径：Environment.getExternalStorageDirectory().getPath()+File.separator+"plugin"+File.separator+"apkplugin.apk"
            addAssetPath.invoke(assetManager, apkDir+File.separator+apkName);//将未安装的Apk文件的添加进AssetManager中，第二个参数为apk文件的路径带apk名
            Resources superRes = this.getResources();
            Resources mResources = new Resources(assetManager, superRes.getDisplayMetrics(),
                    superRes.getConfiguration());
            return mResources;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
//        if (id == R.id.skin) {
//            //第二步先查找并得到该apk目录下所有的apk信息
//            datas = searchAllPlugin(apkDir);
//            //第三步显示查找后可用的apk插件
//            showCanEnabledPlugin(datas);
//            //第四步处理用户的点击事件,并设置相应的皮肤
//            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                @Override
//                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                    HashMap<String,String> map =datas.get(position);
//                    if(map!=null){
//                        String pkgName = map.get("pkgName");
//                        String apkname = apkName.get(position);
//                        try {
//                            //动态加载得到相应的资源
//                            dynamicLoadApk(apkDir, apkname, pkgName);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            });
//        }
        return super.onOptionsItemSelected(item);
    }
    private List<HashMap<String,String>> searchAllPlugin(String apkDir){
        List<HashMap<String,String>> lists = new ArrayList<>();
        File dir = new File(apkDir);
        if(dir.isDirectory()){
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.endsWith(".apk");
                }
            };
            //过滤掉其它文件，只留apk结尾的
            File[] apks =dir.listFiles(filter);
            for (int i = 0; i < apks.length; i++) {
                File temp = apks[i];
                apkName.add(temp.getName());//存储apk名称

                String[] info = getUninstallApkInfo(this,apkDir+File.separator+temp.getName());
                HashMap<String,String> map = new HashMap<>();
                map.put("label",info[0]);
                map.put("pkgName",info[1]);
                lists.add(map);
                map = null;
            }
        }
        return lists;
    }

    private List<HashMap<String,String>> getDemoPlugin(String apkDir){
        List<HashMap<String,String>> lists = new ArrayList<>();
        File dir = new File(apkDir);
        if(dir.isDirectory()){
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return  filename.equals("demo.apk");
                }
            };
            //过滤掉其它文件，只留apk结尾的
            File[] apks =dir.listFiles(filter);
            for (int i = 0; i < apks.length; i++) {
                File temp = apks[i];
                apkName.add(temp.getName());//存储apk名称

                String[] info = getUninstallApkInfo(this,apkDir+File.separator+temp.getName());
                HashMap<String,String> map = new HashMap<>();
                map.put("label",info[0]);
                map.put("pkgName",info[1]);
                lists.add(map);
                map = null;
            }
        }
        return lists;
    }

    /**
     * 列出应用中的可用插件,由于只是示例演示，该demo就简单的把apk插件放在assets目录下，然后运行时候拷贝到sd卡的plugin目录下
     */
    private void showCanEnabledPlugin(List<HashMap<String,String>> datas) {
        if(datas==null||datas.isEmpty()){
            Toast.makeText(this, "Could Not found Plugin(demo.apk)", Toast.LENGTH_SHORT).show();
        }
        View view = LayoutInflater.from(this).inflate(R.layout.layout_item,null,false);
        mListView = (ListView) view.findViewById(R.id.listview);
        mListView.setAdapter(new SimpleAdapter(this, datas, android.R.layout.simple_list_item_1, new String[]{"label"}, new int[]{android.R.id.text1}));
        PopupWindow popupWindow = new PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
        popupWindow.showAtLocation(view, Gravity.TOP | Gravity.RIGHT, 0, 0);
    }

    /**
     * 获取未安装apk的信息
     * @param context
     * @param archiveFilePath apk文件的path
     * @return
     */
    private String[] getUninstallApkInfo(Context context, String archiveFilePath) {
        String[] info = new String[2];
        PackageManager pm = context.getPackageManager();
        PackageInfo pkgInfo = pm.getPackageArchiveInfo(archiveFilePath, PackageManager.GET_ACTIVITIES);
        if (pkgInfo != null) {
            ApplicationInfo appInfo = pkgInfo.applicationInfo;
            String versionName = pkgInfo.versionName;//版本号
            Drawable icon = pm.getApplicationIcon(appInfo);//图标
            String appName = pm.getApplicationLabel(appInfo).toString();//app名称
            String pkgName = appInfo.packageName;//包名
            info[0] = appName;
            info[1] = pkgName;
        }
        return info;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_changebackgound:
                changeBackgound();
                break;

            case R.id.btn_getvalue_from_plugin_method_nop:
                getPluginMethodValue();
                break;

            case R.id.btn_getvalue_from_plugin_method_hasp:
                getPluginMethodValueWithParams();
                break;

            case R.id.btn_getvalue_from_plugin_private_method_nop:
                getPluginPrivateMethodValueNoParams();
                break;
        }
    }

    private void changeBackgound(){
        datas = getDemoPlugin(apkDir);
        //第三步显示查找后可用的apk插件
        if(datas==null||datas.isEmpty()){
            Toast.makeText(this, "Could Not found Plugin(demo.apk)", Toast.LENGTH_SHORT).show();
        }

        HashMap<String,String> map =datas.get(0);
        if(map!=null){
            String pkgName = map.get("pkgName");
            String apkname = apkName.get(0);
            try {
                dynamicLoadApk(apkDir, apkname, pkgName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void getPluginMethodValue(){
        datas = getDemoPlugin(apkDir);
        //第三步显示查找后可用的apk插件
        if(datas==null||datas.isEmpty()){
            Toast.makeText(this, "Could Not found Plugin(demo.apk)", Toast.LENGTH_SHORT).show();
        }

        HashMap<String,String> map =datas.get(0);
        if(map!=null){
            String pkgName = map.get("pkgName");
            String apkname = apkName.get(0);
            try {
                dynamicLoadApkMethodNoParams(apkDir, apkname, pkgName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void getPluginMethodValueWithParams(){
        datas = getDemoPlugin(apkDir);
        //第三步显示查找后可用的apk插件
        if(datas==null||datas.isEmpty()){
            Toast.makeText(this, "Could Not found Plugin(demo.apk)", Toast.LENGTH_SHORT).show();
        }

        HashMap<String,String> map =datas.get(0);
        if(map!=null){
            String pkgName = map.get("pkgName");
            String apkname = apkName.get(0);
            try {
                dynamicLoadApkMethodWithParams(apkDir, apkname, pkgName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void getPluginPrivateMethodValueNoParams(){
        datas = getDemoPlugin(apkDir);
        //第三步显示查找后可用的apk插件
        if(datas==null||datas.isEmpty()){
            Toast.makeText(this, "Could Not found Plugin(demo.apk)", Toast.LENGTH_SHORT).show();
        }

        HashMap<String,String> map =datas.get(0);
        if(map!=null){
            String pkgName = map.get("pkgName");
            String apkname = apkName.get(0);
            try {
                dynamicLoadApkPrivateMethodNoParams(apkDir, apkname, pkgName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
