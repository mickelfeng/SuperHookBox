package com.specher.superhookbox;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Tiktok {
    public Activity mActivity;
    public JSONObject checks;
    public Config config;
    private Class<?> hookClass_LongPressLayout;
    private Class<?> hookClass_VideoViewHolder;
    private Class<?> hookClass_MainFragment;
    private Class<?> hookClass_VideoModle;
    private Class<?> hookClass_BaseListFragmentPanel;
    private Class<?> hookClass_FeedApi;
    private Class<?> hookClass_Adaptation;
    private Class<?> hookClass_CommentColorViewModel;
    private Class<?> hookClass_CommentListFragment;
    private String paramBottomBar;
    private String paramStatusBar;
    private String methodCommentDark;


    private boolean isCommentShow = false;
    private boolean isHide = false;
    private String lastPlaytime = "0";
    private long lastPlay = -1;
    private String lastJumpAid = "0";
    public View llRightMenu;
    public View llAwemeIntro;
    public View mMusicCoverLayout;
    private String downloadAddr;
    private boolean isLite;

    public void hook(final Context context, final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Exception {
        final int versionCode = Utils.getPackageVersionCode(loadPackageParam);
        Utils.log("version:" + versionCode);
        hookClass_LongPressLayout = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.ui.LongPressLayout$2", loadPackageParam.classLoader);
        hookClass_VideoViewHolder = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.adapter.VideoViewHolder", loadPackageParam.classLoader);
        hookClass_MainFragment = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.main.MainFragment", loadPackageParam.classLoader);
        hookClass_VideoModle = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.model.Video", loadPackageParam.classLoader);
        hookClass_BaseListFragmentPanel = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.panel.BaseListFragmentPanel", loadPackageParam.classLoader);
        hookClass_FeedApi = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.api.FeedApi", loadPackageParam.classLoader);

        if (hookClass_BaseListFragmentPanel == null) {

            //BaseListFragmentPanel类被混淆了，可以搜索compiled from: BaseListFragmentPanel
            if (versionCode >= 150701) {
                hookClass_BaseListFragmentPanel = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.panel.c", loadPackageParam.classLoader);
            } else {
                //150301-150601
                hookClass_BaseListFragmentPanel = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.panel.b", loadPackageParam.classLoader);
            }
            //去黑边
            hookClass_Adaptation = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.adaptation.f", loadPackageParam.classLoader);
            paramBottomBar = "j";
            paramStatusBar = "i";
            methodCommentDark = "a";
            hookClass_CommentColorViewModel = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.comment.viewmodel.CommentColorViewModel", loadPackageParam.classLoader);
            hookClass_CommentListFragment = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.comment.ui.CommentListFragment", loadPackageParam.classLoader);

        }


        config = new Config(context, Config.getConfigName(Config.isTikTok));
        checks = config.readPref();
        XposedHelpers.findAndHookMethod("com.ss.android.ugc.aweme.main.MainActivity", loadPackageParam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            public void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (mActivity != null) {
                    mActivity = (Activity) param.thisObject;
                    return;//防止退出MainActivity之后的重复hook
                }
                mActivity = (Activity) param.thisObject;
                if (hookClass_LongPressLayout == null) {
                    Toast.makeText(mActivity, "抖X插件:不支持当前版本:" + versionCode, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(mActivity, "抖X插件:已加载成功。", Toast.LENGTH_SHORT).show();

                    Method onPlayCompletedFirstTime = XposedHelpers.findMethodExactIfExists(hookClass_BaseListFragmentPanel, "onPlayCompletedFirstTime", String.class);
                    if (onPlayCompletedFirstTime != null) {
                        //15.3.0及以下精简版
                        //自动播放
                        isLite = true;
                        XposedBridge.hookMethod(onPlayCompletedFirstTime, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                //防止连续触发两次
                                if (checks.getBoolean(config.isAutoPlay) && !lastPlaytime.equals(param.args[0])) {
                                    lastPlaytime = (String) param.args[0];
                                    Object mViewPager = XposedHelpers.getObjectField(param.thisObject, "mViewPager");
                                    XposedHelpers.callMethod(mViewPager, "setCurrentItem", (int) XposedHelpers.callMethod(mViewPager, "getCurrentItem") + 1);
                                }
                                super.beforeHookedMethod(param);
                            }
                        });

                        Class<?> VideoViewHolder = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.adapter.VideoViewHolder", loadPackageParam.classLoader);
                        if (VideoViewHolder != null) {
                            XposedBridge.hookAllConstructors(VideoViewHolder, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    //精简版隐藏右侧按钮和文字，可能后续会失效，懒得改了
                                    try {
                                        XposedHelpers.callMethod(param.thisObject, "l", checks.getBoolean(config.hideRightMenu));
                                    } catch (Exception e) {
                                        Utils.log(e.getMessage());
                                    }
                                    super.afterHookedMethod(param);
                                }
                            });
                        }
                        //无水印下载
                        XposedHelpers.findAndHookMethod(hookClass_VideoModle, "getDownloadAddr", new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                if (checks.getBoolean(config.downLoadVideo)) {
                                    XposedHelpers.callMethod(mActivity, "showCustomToast", "已开启无水印下载。");
                                    //Toast.makeText(mActivity,"已开启无水印下载。",Toast.LENGTH_SHORT).show();
                                    param.setResult(XposedHelpers.callMethod(param.thisObject, "getPlayAddr"));
                                }
                                super.afterHookedMethod(param);
                            }
                        });
                    } else {
                        //自动播放
                        XposedBridge.hookAllMethods(hookClass_BaseListFragmentPanel, "onVideoPlayerEvent", new XC_MethodHook() {
                            public void afterHookedMethod(MethodHookParam param) throws Throwable {
                                super.afterHookedMethod(param);
                                //com.ss.android.ugc.aweme.shortvideo.f.i VideoPlayerStatus=7代表播放完成
                                String filedName = XposedHelpers.findFirstFieldByExactType(param.args[0].getClass(), int.class).getName();
                                if (checks.getBoolean(config.isAutoPlay) && XposedHelpers.getIntField(param.args[0], filedName) == 7 && (!checks.getBoolean(config.isAutoPlayC) || !isCommentShow)) {
                                    //VerticalViewPager
                                    String VerticalViewPagerfiledName = XposedHelpers.findFirstFieldByExactType(param.thisObject.getClass(),
                                            XposedHelpers.findClass("com.ss.android.ugc.aweme.common.widget.VerticalViewPager", loadPackageParam.classLoader)).getName();
                                    Object mViewPager = XposedHelpers.getObjectField(param.thisObject, VerticalViewPagerfiledName);
                                    int currItem = (Integer) XposedHelpers.callMethod(mViewPager, "getCurrentItem");
                                    XposedHelpers.callMethod(mViewPager, "setCurrentItem", currItem + 1);
                                }
                            }
                        });


                        //跳过视频流广告和隐藏切换辅助
                        XposedBridge.hookAllMethods(mActivity.getClass(), "onVideoPageChangeEvent", new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                                //OnVideoPageChangeEvent
                                //class com.ss.android.ugc.aweme.feed.h.av
                                //public Aweme a;
                                final Object Aweme = XposedHelpers.getObjectField(param.args[0], "a");

                                Object mCurFragment = XposedHelpers.callMethod(param.thisObject, "getCurFragment");
                                String tmpAid = (String) XposedHelpers.callMethod(Aweme, "getAid");
                                final int awemeType = (int) XposedHelpers.callMethod(Aweme, "getAwemeType");
                                if (awemeType==101?checks.getBoolean(config.jumpLive):checks.getBoolean(config.jumpAD) && (boolean) XposedHelpers.callMethod(Aweme, "isAd") && !lastJumpAid.equals(tmpAid)) {
                                    //FeedRecommendFragment
                                    Object FeedRecommendFragment = XposedHelpers.callMethod(mCurFragment, "a");
                                    //BaseListFragmentPanel
                                    Object baseListFragmentPanel = XposedHelpers.callMethod(FeedRecommendFragment,
                                            Utils.findMethodbyReturnType(FeedRecommendFragment.getClass(), hookClass_BaseListFragmentPanel.getName()).getName());
                                    //VerticalViewPager
                                    String VerticalViewPagerfiledName = XposedHelpers.findFirstFieldByExactType(hookClass_BaseListFragmentPanel,
                                            XposedHelpers.findClass("com.ss.android.ugc.aweme.common.widget.VerticalViewPager", loadPackageParam.classLoader)).getName();
                                    Object mViewPager = XposedHelpers.getObjectField(baseListFragmentPanel, VerticalViewPagerfiledName);
                                    int currItem = (Integer) XposedHelpers.callMethod(mViewPager, "getCurrentItem");
                                    XposedHelpers.callMethod(mViewPager, "setCurrentItem", currItem + 1);
                                    lastJumpAid = tmpAid;
                                    if (checks.getBoolean(config.jumpADTip)) {
                                        mActivity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                XposedHelpers.callMethod(param.thisObject, "showCustomToast", awemeType == 101 ? "跳过直播" : "跳过广告：" + XposedHelpers.callMethod(Aweme, "getDesc"));
                                            }
                                        });
                                        Utils.log("跳过广告：" + Aweme);
                                    }
                                }
                                Object VideoViewHolder = XposedHelpers.callMethod(param.thisObject, "getCurrentViewHolder");
                                if (VideoViewHolder != null) {
                                    llRightMenu = (View) XposedHelpers.getObjectField(VideoViewHolder, "llRightMenu");
                                    llAwemeIntro = (View) XposedHelpers.getObjectField(VideoViewHolder, "llAwemeIntro");
                                    mMusicCoverLayout = (View) XposedHelpers.getObjectField(VideoViewHolder, "mMusicCoverLayout");
                                    if (llRightMenu != null) {
                                        llAwemeIntro.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                //隐藏切换
                                                if (isHide) {
                                                    try {
                                                        if (checks.getBoolean(config.hideRightMenu)) {
                                                            llRightMenu.setAlpha(0);
                                                            mMusicCoverLayout.setVisibility(View.INVISIBLE);
                                                        }
                                                        if (checks.getBoolean(config.hideAwemeIntro)) {
                                                            llAwemeIntro.setVisibility(View.INVISIBLE);
                                                        }
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                } else {
                                                    llRightMenu.setAlpha(1);
                                                    llAwemeIntro.setVisibility(View.VISIBLE);
                                                    mMusicCoverLayout.setVisibility(View.VISIBLE);
                                                }
                                            }
                                        });
                                    }


                                }
                                super.beforeHookedMethod(param);
                            }
                        });

                        //无水印下载
                        XposedHelpers.findAndHookMethod(hookClass_VideoModle, "getDownloadAddr", new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                if (checks.getBoolean(config.downLoadVideo)) {
                                    Object playAddr = XposedHelpers.callMethod(param.thisObject, "getPlayAddr");
                                    List<String> urlList = (List<String>) XposedHelpers.callMethod(playAddr, "getUrlList");
                                    //Utils.log("DownloadInfo:urlList" + urlList);
                                    if (urlList.size() > 0) {
                                        downloadAddr = urlList.get(0);
                                        //Utils.log("DownloadInfo:downloadAddr" + downloadAddr);
                                    }
                                }
                                super.afterHookedMethod(param);
                            }
                        });
                        Class<?> DownloadTask = XposedHelpers.findClassIfExists("com.ss.android.socialbase.downloader.model.DownloadTask", loadPackageParam.classLoader);
                        if (DownloadTask != null) {
                            XposedHelpers.findAndHookMethod(DownloadTask, "download", new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if (downloadAddr != null) {
                                        Object DownloadInfo = XposedHelpers.callMethod(param.thisObject, "getDownloadInfo");
                                        XposedHelpers.callMethod(DownloadInfo, "setUrl", downloadAddr);
                                        //Utils.log("DownloadInfo:" + DownloadInfo);
                                        mActivity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(mActivity, "已开启无水印下载。", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                        downloadAddr = null;
                                    }
                                    super.afterHookedMethod(param);
                                }
                            });
                        }

                        //去黑边
                        if (hookClass_Adaptation != null) {
                            XposedHelpers.findAndHookMethod(hookClass_Adaptation, "run", new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    if (checks.getBoolean(config.isFullScreen)) {
                                        XposedHelpers.setIntField(param.thisObject, paramStatusBar, 0);
                                        XposedHelpers.setIntField(param.thisObject, paramBottomBar, 0);
                                    }
                                    super.beforeHookedMethod(param);
                                }
                            });

                        }
                        //评论暗黑
                        if (hookClass_CommentColorViewModel != null) {
                            XposedHelpers.findAndHookMethod(hookClass_CommentColorViewModel, methodCommentDark, Integer.TYPE, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    if (checks.getBoolean(config.isCommentDark)) {
                                        param.args[0] = 2;
                                    }
                                    super.beforeHookedMethod(param);
                                }
                            });
                        }

                        //评论是否打开
                        if (hookClass_CommentListFragment != null) {
                            Method onCommentListOpen = XposedHelpers.findMethodExactIfExists(hookClass_CommentListFragment, "b", Activity.class);
                            Method onCommentListClose = XposedHelpers.findMethodExactIfExists(hookClass_CommentListFragment, "c", Activity.class);
                            if (onCommentListOpen != null) {
                                XposedBridge.hookMethod(onCommentListOpen, new XC_MethodHook() {
                                    public void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        isCommentShow = true;
                                        super.afterHookedMethod(param);

                                    }
                                });
                            }
                            if (onCommentListClose != null) {
                                XposedBridge.hookMethod(onCommentListClose, new XC_MethodHook() {
                                    public void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        isCommentShow = false;
                                        super.afterHookedMethod(param);

                                    }
                                });
                            }
                        }

                    }

                    //长按Hook
                    XposedHelpers.findAndHookMethod(hookClass_LongPressLayout, "run", new XC_MethodReplacement() {
                        @SuppressLint("ResourceType")
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            Utils.log("触发LongPress");
                            if (checks.getBoolean(config.fullVideoPlay)) {

                                Object mCurFragment = XposedHelpers.callMethod(mActivity, "getCurFragment");
                                Object MainPageFragment;

                                //顶部Tab
                                View mPagerTabStrip = null;
                                View a = null, b = null, c = null;
                                //底部TabView
                                FrameLayout mMainBottomTabView = null;
                                if (isLite) {
                                    if (checks.getBoolean(config.hideTopTab)) {
                                        //判断是否是首页Fragment)
                                        if (mCurFragment.getClass().equals(hookClass_MainFragment)) {
                                            mPagerTabStrip = (View) XposedHelpers.getObjectField(mCurFragment, "mPagerTabStrip");
                                            //顶部故事和相机按钮
                                            a = (View) XposedHelpers.getObjectField(mCurFragment, "mIvBtnStoryCamera");
                                            b = (View) XposedHelpers.getObjectField(mCurFragment, "mIvBtnStorySwitch");
                                            //顶部搜索按钮
                                            c = (View) XposedHelpers.getObjectField(mCurFragment, "mIvBtnSearch");
                                        }
                                    }
                                    if (checks.getBoolean(config.hideBottomTab)) {
                                        mMainBottomTabView = (FrameLayout) XposedHelpers.getObjectField(mActivity, "mMainBottomTabView");
                                    }
                                } else {
                                    //15.4.0使用enterDislikeMode里的方法控制顶部Tab
                                    //HomePageUIFrameServiceImpl.createHomePageUIFrameServicebyMonsterPlugin(false).setTitleTabVisibility(false);
                                    if (checks.getBoolean(config.hideTopTab)) {
                                        XposedHelpers.callMethod(XposedHelpers.callStaticMethod(
                                                XposedHelpers.findClass("com.ss.android.ugc.aweme.main.uiApiImpl.HomePageUIFrameServiceImpl"
                                                        , loadPackageParam.classLoader), "createHomePageUIFrameServicebyMonsterPlugin"
                                                , false), "setTitleTabVisibility", isHide);
                                    }
                                    MainPageFragment = XposedHelpers.callMethod(mActivity, "getMainPageFragment");
                                    mMainBottomTabView = (FrameLayout) XposedHelpers.getObjectField(MainPageFragment, "mMainBottomTabView");
                                }

                                //隐藏切换
                                if (!isHide) {
                                    //隐藏通知栏
                                    if (checks.getBoolean(config.hideStatusBar)) {
                                        //XposedHelpers.callMethod(mActivity, "hideStatusBar");
                                        mActivity.getWindow().setFlags(1024, 1024);
                                    }
                                    if (checks.getBoolean(config.hideRightMenu) && llRightMenu != null) {
                                        llRightMenu.setAlpha(0);
                                        mMusicCoverLayout.setVisibility(View.INVISIBLE);
                                    }
                                    if (checks.getBoolean(config.hideAwemeIntro) && llAwemeIntro != null) {
                                        llAwemeIntro.setVisibility(View.INVISIBLE);
                                    }
                                    if (mPagerTabStrip != null) {
                                        mPagerTabStrip.setVisibility(View.GONE);
                                        a.setVisibility(View.GONE);
                                        b.setVisibility(View.GONE);
                                        c.setVisibility(View.GONE);
                                    }
                                    if (mMainBottomTabView != null) {
                                        mMainBottomTabView.setVisibility(View.GONE);
                                    }
                                    isHide = true;
                                } else {
                                    if (mPagerTabStrip != null) {
                                        mPagerTabStrip.setVisibility(View.VISIBLE);
                                        a.setVisibility(View.VISIBLE);
                                        b.setVisibility(View.VISIBLE);
                                        c.setVisibility(View.VISIBLE);
                                    }
                                    if (mMainBottomTabView != null) {
                                        mMainBottomTabView.setVisibility(View.VISIBLE);
                                    }
                                    if (llRightMenu != null) {
                                        llRightMenu.setAlpha(1);
                                        llAwemeIntro.setVisibility(View.VISIBLE);
                                        mMusicCoverLayout.setVisibility(View.VISIBLE);
                                    }
                                    mActivity.getWindow().clearFlags(1024);
                                    //XposedHelpers.callMethod(mActivity, "showStatusBar");
                                    isHide = false;
                                }
                            } else {
                                XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
                            }
                            return null;
                        }
                    });


                }
            }
        });

        XposedHelpers.findAndHookMethod("com.ss.android.ugc.aweme.main.MainActivity", loadPackageParam.classLoader, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                //刷新配置
                if (config != null) {
                    checks = config.readPref();
                }
                super.afterHookedMethod(param);
            }
        });

        //去开屏广告
        if (checks.getBoolean(config.isJumpSplashAd)) {
            Class<?> hookClass_SplashAdActivity;
            hookClass_SplashAdActivity = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.splash.SplashAdActivity", loadPackageParam.classLoader);
                   if(hookClass_SplashAdActivity!=null) {
                       XposedBridge.hookAllMethods(hookClass_SplashAdActivity, "onCreate", new XC_MethodHook() {
                           @Override
                           protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                               ((Activity) param.thisObject).finish();
                               Utils.log("跳过开屏广告一个");
                               super.beforeHookedMethod(param);
                           }
                       });
                   }
                   Class <?> hookClass_CommercializeSplashAdActivity =  XposedHelpers.findClassIfExists("com.bytedance.ies.ugc.aweme.commercialize.splash.show.SplashAdActivity", loadPackageParam.classLoader);
                 XposedBridge.hookAllMethods(hookClass_CommercializeSplashAdActivity, "onCreate", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    ((Activity) param.thisObject).finish();
                    Utils.log("跳过开屏广告一个");
                    super.beforeHookedMethod(param);
                }
            });

            //method_splash_try_show_ad_duration
             Method  SplashTryShowAd=  XposedHelpers.findMethodExactIfExists("com.ss.android.ugc.aweme.splash.h", loadPackageParam.classLoader, "a", Activity.class, Bundle.class);
            if(SplashTryShowAd!=null) {
                XposedBridge.hookMethod(SplashTryShowAd, new XC_MethodReplacement() {
                    public Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                        return Boolean.FALSE;
                    }
                });
            }
//
//            失效
//            XposedHelpers.findAndHookMethod(View.class, "setLayoutParams", ViewGroup.LayoutParams.class, new XC_MethodHook() {
//                @Override
//                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                    if (param.args[0] == null) {
//                        return;
//                    }
//                    final View view = (View) param.thisObject;
//                    if (view instanceof TextView) {
//                        try {
//                            final TextView tmp = (TextView) view;
//                            if (tmp.getText().toString().contains("跳过") ) {
//                                //final ViewGroup.LayoutParams layoutParams = (ViewGroup.LayoutParams) param.args[0];
//                                //final Context context = tmp.getRootView().getContext();
//                                tmp.post(new Runnable() {
//                                    @RequiresApi(api = Build.VERSION_CODES.M)
//                                    @Override
//                                    public void run() {
//                                        tmp.performClick();
//                                    }
//                                });
//                            }
//                        }catch (Exception e){
//                            Utils.log(e.getMessage());
//                        }
//                    }
//                    super.afterHookedMethod(param);
//                }
//
//
//            });

        }


    }
}