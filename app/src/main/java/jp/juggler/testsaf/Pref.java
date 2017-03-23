package jp.juggler.testsaf;

import android.content.Context;
import android.content.SharedPreferences;

public class Pref{

	public static SharedPreferences pref( Context context ){
		return context.getSharedPreferences( "app_pref", Context.MODE_PRIVATE );
	}

	public static final String UI_PRIMARY_STORAGE = "ui_primary_storage";
	public static final String UI_SECONDARY_STORAGE = "ui_secondary_storage";

	public static final String WIDGET_IMAGE_URI = "widget_image_uri";
}
