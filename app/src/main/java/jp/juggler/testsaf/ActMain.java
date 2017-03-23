package jp.juggler.testsaf;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import jp.juggler.testsaf.permission.PermissionChecker;
import jp.juggler.testsaf.utils.FileCreator;
import jp.juggler.testsaf.utils.LocalFile;
import jp.juggler.testsaf.utils.LogWriter;
import jp.juggler.testsaf.utils.Utils;

public class ActMain extends AppCompatActivity
	implements View.OnClickListener
{

	static final LogWriter log = new LogWriter( "ActMain" );

	static final int REQUEST_CODE_APP_PERMISSION = 1;
	static final int REQUEST_CODE_PRIMARY_STORAGE = 2;
	static final int REQUEST_CODE_SECONDARY_STORAGE = 3;
	static final int REQUEST_CODE_SHARE = 4;

	@Override public void onClick( View view ){
		switch( view.getId() ){

		case R.id.btnPrimaryStorage:
			LocalFile.openFolderPicker(
				this
				, REQUEST_CODE_PRIMARY_STORAGE
				, tvPrimaryStorage.getText().toString()
			);
			break;

		case R.id.btnSecondaryStorage:
			LocalFile.openFolderPicker(
				this
				, REQUEST_CODE_SECONDARY_STORAGE
				, tvSecondaryStorage.getText().toString()
			);
			break;

		case R.id.btnShare:
			try{
				FileCreator fc = creator_list.get( spShareFrom.getSelectedItemPosition() );
				String file = fc.createFile();
				Uri uri = changeUri( file, spShareUriType.getSelectedItemPosition()
					, fc.is_external
				);
				if( uri != null ){
					Pref.pref( this ).edit().putString( Pref.WIDGET_IMAGE_URI, uri.toString() ).apply();
					WidgetHasImage.updateWidget( this );

					tvLastShare.setText( uri.toString() );
					Intent intent = new Intent( Intent.ACTION_VIEW );
					intent.setDataAndType( uri, Utils.getMimeType( file ) ); // MimeTypeMapが使われる…
					intent.addFlags(
						Intent.FLAG_GRANT_WRITE_URI_PERMISSION
							| Intent.FLAG_GRANT_READ_URI_PERMISSION
					);
					startActivityForResult( intent, REQUEST_CODE_SHARE );
				}
			}catch( Throwable ex ){
				ex.printStackTrace();
				Utils.showToast( this, ex, "share failed" );
			}
			break;
		case R.id.btnWidget:
			try{
				FileCreator fc = creator_list.get( spShareFrom.getSelectedItemPosition() );
				String str_file = fc.createFile();
				File file = Utils.getFile( this, str_file );
				if( file == null ){
					Utils.showToast( this, true, "can't get file path from %s", str_file );
				}else{
					if( ! WidgetHasImage.updateImage( this, file ) ){
						WidgetHasImage.deleteImage( this );
					}
				}
			}catch( Throwable ex ){
				ex.printStackTrace();
				Utils.showToast( this, ex, "widget updateWidget failed." );
			}
			break;
		}
	}

	boolean is_resume;

	@Override protected void onResume(){
		is_resume = true;
		super.onResume();
		permission_checker.permission_request( REQUEST_CODE_APP_PERMISSION );
	}

	@Override public void onRequestPermissionsResult( int requestCode, @NonNull String permissions[], @NonNull int[] grantResults ){
		if( requestCode == REQUEST_CODE_APP_PERMISSION ){
			permission_checker.permission_request( REQUEST_CODE_APP_PERMISSION );
		}
	}

	@SuppressLint( "NewApi" )
	@Override public void onActivityResult( int requestCode, int resultCode, Intent resultData ){

		if( requestCode == REQUEST_CODE_PRIMARY_STORAGE ){
			String local_file = LocalFile.handleFolderPickerResult( this, resultCode, resultData );
			if( ! TextUtils.isEmpty( local_file ) ){
				// 覚えておく
				Pref.pref( this ).edit()
					.putString( Pref.UI_PRIMARY_STORAGE, local_file )
					.apply();
				// 表示を更新
				updateFolderView();
			}
			return;
		}

		if( requestCode == REQUEST_CODE_SECONDARY_STORAGE ){
			String local_file = LocalFile.handleFolderPickerResult( this, resultCode, resultData );
			if( ! TextUtils.isEmpty( local_file ) ){
				// 覚えておく
				Pref.pref( this ).edit()
					.putString( Pref.UI_SECONDARY_STORAGE, local_file )
					.apply();
				// 表示を更新
				updateFolderView();
			}
			return;
		}

		super.onActivityResult( requestCode, resultCode, resultData );
	}

	TextView tvPrimaryStorage;
	TextView tvSecondaryStorage;
	Spinner spShareUriType;
	Spinner spShareFrom;
	TextView tvLastShare;

	PermissionChecker permission_checker;
	ArrayList<FileCreator> creator_list;

	@Override
	protected void onCreate( Bundle savedInstanceState ){
		super.onCreate( savedInstanceState );

		creator_list = FileCreator.getFileCreatorList( getApplicationContext() );

		permission_checker = new PermissionChecker( this );

		setContentView( R.layout.act_main );

		tvPrimaryStorage = (TextView) findViewById( R.id.tvPrimaryStorage );
		findViewById( R.id.btnPrimaryStorage ).setOnClickListener( this );

		tvSecondaryStorage = (TextView) findViewById( R.id.tvSecondaryStorage );
		findViewById( R.id.btnSecondaryStorage ).setOnClickListener( this );

		spShareFrom = (Spinner) findViewById( R.id.spShareFrom );
		spShareUriType = (Spinner) findViewById( R.id.spShareUriType );
		findViewById( R.id.btnShare ).setOnClickListener( this );
		tvLastShare = (TextView) findViewById( R.id.tvLastShare );
		findViewById( R.id.btnWidget ).setOnClickListener( this );

		{
			ArrayAdapter<String> share_from_adapter = new ArrayAdapter<>(
				this
				, android.R.layout.simple_spinner_item
			);
			share_from_adapter.setDropDownViewResource( R.layout.spinner_dropdown );
			for( FileCreator fc : creator_list ){
				share_from_adapter.add( fc.name );
			}
			spShareFrom.setAdapter( share_from_adapter );
		}
		{
			ArrayAdapter<String> share_uri_type_adapter = new ArrayAdapter<>(
				this
				, android.R.layout.simple_spinner_item
			);
			share_uri_type_adapter.setDropDownViewResource( R.layout.spinner_dropdown );
			share_uri_type_adapter.add( "File URI" );
			share_uri_type_adapter.add( "SAF content URI" );
			share_uri_type_adapter.add( "FileProvider URI" );
			share_uri_type_adapter.add( "MediaStore URI" );
			spShareUriType.setAdapter( share_uri_type_adapter );
		}

		// Example of a call to a native method
//		TextView tv = (TextView) findViewById( R.id.sample_text );
//		tv.setText( stringFromJNI() );
//		final EditText editTextUserInput = (EditText) findViewById( R.id.editTextUserInput );
//		final Button btnTestSample1 = (Button) findViewById( R.id.btnTestSample1 );
//		btnTestSample1.setOnClickListener( new View.OnClickListener(){
//			@Override public void onClick( View v ){
//				btnTestSample1.setText( editTextUserInput.getText() );
//			}
//		} );

		updateFolderView();
	}

//	// Used to load the 'native-lib' library on application startup.
//	static{
//		System.loadLibrary( "native-lib" );
//	}
//
//	/**
//	 * A native method that is implemented by the 'native-lib' native library,
//	 * which is packaged with this application.
//	 */
//	public native String stringFromJNI();

	private void updateFolderView(){
		SharedPreferences pref = Pref.pref( this );
		String sv;

		//
		sv = pref.getString( Pref.UI_PRIMARY_STORAGE, "" );
		tvPrimaryStorage.setText( sv );

		//
		sv = pref.getString( Pref.UI_SECONDARY_STORAGE, "" );
		tvSecondaryStorage.setText( sv );
	}

	private Uri changeUri( String src, int mode, boolean is_external ){
		Uri uri;
		File file;
		switch( mode ){

		case 0: //File URI
			if( ! is_external ){
				Utils.showToast( this, true, "shall not share the file in internal storage" );
				break;
			}

			file = Utils.getFile( this, src );
			if( file == null ){
				Utils.showToast( this, true, "can't get file path from %s", src );
				break;
			}
			return Uri.fromFile( file );

		case 1: // SAF content URI
			if( src.startsWith( "/" ) ){
				Utils.showToast( this, true, "can't get SAF content URI from %s", src );
				break;
			}
			uri = Uri.parse( src );
			if( "file".equals( uri.getScheme() ) ){
				Utils.showToast( this, true, "can't get SAF content URI from %s", src );
				break;
			}
			return uri;

		case 2: // FileProvider URI
			file = Utils.getFile( this, src );
			if( file == null ){
				Utils.showToast( this, true, "can't get file path from %s", src );
				break;
			}
			uri = FileProvider.getUriForFile( this, "jp.juggler.testsaf.fileprovider", file );
			// LGV32(Android 6.0)でSDカードを使うと例外発生
			// IllegalArgumentException: Failed to find configured root that contains /storage/3136-6334/image1.jpg
			// ワークアラウンド： FileProviderに指定するpath xml に <root-path  name="pathRoot" path="." /> を追加
			if( uri == null ){
				Utils.showToast( this, true, "can't get FileProvider URI from %s", file.getAbsolutePath() );
				break;
			}else{
				Cursor cursor = getContentResolver().query(
					uri
					, null
					, null
					, null
					, null
				);
				if( cursor != null ){
					try{
						if( cursor.moveToFirst() ){
							int col_count = cursor.getColumnCount();
							for( int i = 0 ; i < col_count ; ++ i ){
								int type = cursor.getType( i );
								if( type != Cursor.FIELD_TYPE_STRING ) continue;
								String name = cursor.getColumnName( i );
								String value = cursor.isNull( i ) ? null : cursor.getString( i );
								log.d( "FileProvider data: %s %s", name, value );
							}
						}else{
							Utils.showToast( this, true, "invalid FileProvider URI. %s", file.getAbsolutePath() );
							break;
						}
					}finally{
						cursor.close();
					}
				}
			}
			return uri;

		case 3: // MediaStore URI

			if( ! is_external ){
				Utils.showToast( this, true, "shall not share the file in internal storage" );
				break;
			}

			file = Utils.getFile( this, src );
			if( file == null ){
				Utils.showToast( this, true, "can't get file path from %s", src );
				break;
			}

			uri = Utils.registerMediaURI( this, file, true );
			if( uri == null ){
				Utils.showToast( this, true, "can't register media URI for %s", file.getAbsolutePath() );
				break;
			}
			return uri;
		}
		return null;
	}

}
