package eu.lighthouselabs.obd;

import android.app.Application;

/**
 * @author caixingcun
 * @date 2021/11/17
 * Description :
 */
public class App extends Application {
    static Application instance;
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static Application getInstance() {
        return instance;
    }
}
