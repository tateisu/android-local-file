package jp.juggler.testsaf.permission;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class PermissionChecker{

	private static final String[] permission_list = new String[]{
		Manifest.permission.WRITE_EXTERNAL_STORAGE,
	};

	final Activity activity;
	private WeakReference<Dialog> permission_alert;

	public PermissionChecker( Activity activity ){
		this.activity = activity;
	}

	public ArrayList<String> getMissingPermissionList(){
		ArrayList<String> list = new ArrayList<>();
		if( Build.VERSION.SDK_INT >= 23 ){
			for( String p : permission_list ){
				int r = ContextCompat.checkSelfPermission( activity, p );
				if( r != PackageManager.PERMISSION_GRANTED ){
					list.add( p );
				}
			}
		}
		return list;
	}

	public boolean permission_request( final int request_code ){
		final ArrayList<String> missing_permission_list = getMissingPermissionList();
		if( missing_permission_list.isEmpty() ) return true;

		Dialog dialog = null;
		if( permission_alert != null ) dialog = permission_alert.get();

		// 既にダイアログを表示中なら何もしない
		if( dialog == null || ! dialog.isShowing() ){

			dialog = new AlertDialog.Builder( activity )
				.setMessage( "パーミッションが必要っぽい？" )
				.setPositiveButton( "要求", new DialogInterface.OnClickListener(){
					@Override public void onClick( DialogInterface dialogInterface, int i ){
						ActivityCompat.requestPermissions(
							activity
							, missing_permission_list.toArray( new String[ missing_permission_list.size() ] )
							, request_code
						);
					}
				} )
				.setNegativeButton( "キャンセル", new DialogInterface.OnClickListener(){
					@Override public void onClick( DialogInterface dialogInterface, int i ){
						activity.finish();
					}
				} )
				.setNeutralButton( "アプリ設定", new DialogInterface.OnClickListener(){
					@Override public void onClick( DialogInterface dialogInterface, int i ){
						Intent intent = new Intent();
						intent.setAction( Settings.ACTION_APPLICATION_DETAILS_SETTINGS );
						intent.setData( Uri.parse( "package:" + activity.getPackageName() ) );
						activity.startActivity( intent );
					}
				} )
				.setOnCancelListener( new DialogInterface.OnCancelListener(){
					@Override public void onCancel( DialogInterface dialogInterface ){
						activity.finish();
					}
				} )
				.create();
			dialog.show();
			permission_alert = new WeakReference<>( dialog );

		}

		return false;
	}
}
