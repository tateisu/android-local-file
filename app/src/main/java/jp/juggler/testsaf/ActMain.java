package jp.juggler.testsaf;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import jp.juggler.testsaf.permission.PermissionChecker;
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
				Uri uri = changeUri( file, spShareUriType.getSelectedItemPosition() ,fc.is_external );
				if( uri != null ){
					tvLastShare.setText(uri.toString());
					Intent intent = new Intent( Intent.ACTION_VIEW );
					intent.setDataAndType( uri, Utils.getMimeType( file ) );

					intent.setDataAndType(uri, Utils.getMimeType( file )); // MimeTypeMapが使われる…
					intent.addFlags(
						Intent.FLAG_GRANT_WRITE_URI_PERMISSION
							| Intent.FLAG_GRANT_READ_URI_PERMISSION
					);
					startActivityForResult(intent,REQUEST_CODE_SHARE);
				}
			}catch( Throwable ex ){
				ex.printStackTrace();
				Utils.showToast( this, ex, "share failed" );
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

		creator_list = getFileCreatorList();

		permission_checker = new PermissionChecker( this );

		setContentView( R.layout.act_main );

		tvPrimaryStorage = (TextView) findViewById( R.id.tvPrimaryStorage );
		findViewById( R.id.btnPrimaryStorage ).setOnClickListener( this );

		tvSecondaryStorage = (TextView) findViewById( R.id.tvSecondaryStorage );
		findViewById( R.id.btnSecondaryStorage ).setOnClickListener( this );

		spShareFrom = (Spinner) findViewById( R.id.spShareFrom );
		spShareUriType = (Spinner) findViewById( R.id.spShareUriType );
		findViewById( R.id.btnShare ).setOnClickListener( this );
		tvLastShare= (TextView) findViewById( R.id.tvLastShare );

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

	public String getPrimaryStorage(){
		SharedPreferences pref = Pref.pref( this );
		return pref.getString( Pref.UI_PRIMARY_STORAGE, "" );
	}

	public String getSecondaryStorage(){
		SharedPreferences pref = Pref.pref( this );
		return pref.getString( Pref.UI_SECONDARY_STORAGE, "" );
	}

	String saveImage( File dir ) throws Exception{
		File file = new File( dir, "image1.jpg" );

		InputStream is = this.getAssets().open( "image1.jpg" );
		try{
			FileOutputStream os = new FileOutputStream( file );
			try{
				IOUtils.copy( is, os );
			}finally{
				try{
					os.close();
				}catch( Throwable ignored ){

				}
			}
		}finally{
			try{
				is.close();
			}catch( Throwable ignored ){

			}
		}

		return file.getAbsolutePath();
	}

	String saveImage( DocumentFile dir ) throws Exception{
		String file_name = "image1.jpg";
		DocumentFile file = null;
		for( DocumentFile df : dir.listFiles() ){
			if( file_name.equals( df.getName() ) ){
				file = df;
				break;
			}
		}
		if( file == null ){
			file = dir.createFile( "image/jpeg", file_name );
		}
		Uri file_uri = file.getUri();

		InputStream is = this.getAssets().open( "image1.jpg" );
		try{
			OutputStream os = this.getContentResolver().openOutputStream( file_uri );
			if( os == null ) return null;
			try{
				IOUtils.copy( is, os );
			}finally{
				try{
					os.close();
				}catch( Throwable ignored ){
				}
			}
		}finally{
			try{
				is.close();
			}catch( Throwable ignored ){
			}
		}
		return file_uri.toString();
	}

	String saveImage( String dir ) throws Exception{

		if( dir.startsWith( "/" ) ) return saveImage( new File( dir ) );

		Uri uri = Uri.parse( dir );

		if( uri.getScheme().equals( "file" ) ) return saveImage( new File( uri.getPath() ) );

		DocumentFile df = DocumentFile.fromTreeUri( this, uri );
		return saveImage( df );
	}

	String createSubDirectory( File parent, String name ) throws Exception{
		File dir = new File( parent, name );
		if( ! dir.mkdir() && ! dir.isDirectory() ){
			throw new RuntimeException( String.format( "directory creation failed. %s", dir.getAbsolutePath() ) );
		}
		return dir.getAbsolutePath();
	}

	String createSubDirectory( String parent, String name ) throws Exception{

		if( parent.startsWith( "/" ) ) return createSubDirectory( new File( parent ), name );

		Uri uri = Uri.parse( parent );
		if( uri.getScheme().equals( "file" ) ) return createSubDirectory( new File( uri.getPath() ), name );

		File path = Utils.getFile( this, parent );
		if( path == null ){
			throw new RuntimeException( String.format( "can not get path from uri. %s", parent ) );
		}
		return createSubDirectory( path, name );
	}

	abstract class FileCreator{

		boolean is_external;
		final String name;

		FileCreator( boolean is_external,String name ){
			this.is_external = is_external;
			this.name = name;
		}

		abstract String createFile() throws Exception;

		abstract String createDirectory() throws Exception;

	}

	ArrayList<FileCreator> getFileCreatorList(){
		ArrayList<FileCreator> result = new ArrayList<>();
		result.add(
			new FileCreator( false,"Context#getFilesDir" ){
				File getDir(){
					File dir = ActMain.this.getFilesDir();
					if(dir!=null){
						//noinspection ResultOfMethodCallIgnored
						dir.mkdir();
					}
					return dir;
				}

				@Override String createFile() throws Exception{
					return saveImage( getDir() );
				}

				@Override String createDirectory() throws Exception{
					return createSubDirectory( getDir(), "test_dir" );
				}
			} );

		result.add( new FileCreator(  false,"Context#getCacheDir" ){
			File getDir(){
				File dir = ActMain.this.getCacheDir();
				if(dir!=null){
					//noinspection ResultOfMethodCallIgnored
					dir.mkdir();
				}
				return dir;
			}

			@Override String createFile() throws Exception{
				return saveImage( getDir() );
			}

			@Override String createDirectory() throws Exception{
				return createSubDirectory( getDir(), "test_dir" );
			}
		} );
		result.add( new FileCreator( true, "Context#getExternalFilesDir" ){
			File getDir(){
				File dir = ActMain.this.getExternalFilesDir( Environment.DIRECTORY_DOWNLOADS );
				if(dir!=null){
					//noinspection ResultOfMethodCallIgnored
					dir.mkdir();
				}
				return dir;
			}

			@Override String createFile() throws Exception{
				return saveImage( getDir() );
			}

			@Override String createDirectory() throws Exception{
				return createSubDirectory( getDir(), "test_dir" );
			}
		} );
		result.add( new FileCreator(  true,"Context#getExternalCacheDir" ){
			File getDir(){
				File dir = ActMain.this.getExternalCacheDir();
				if(dir!=null){
					//noinspection ResultOfMethodCallIgnored
					dir.mkdir();
				}
				return dir;
			}

			@Override String createFile() throws Exception{
				return saveImage( getDir() );
			}

			@Override String createDirectory() throws Exception{
				return createSubDirectory( getDir(), "test_dir" );
			}

		} );
		result.add( new FileCreator(  true,"Environment#getExternalStorageDirectory" ){
			File getDir(){
				File dir =  Environment.getExternalStorageDirectory();
				if(dir!=null){
					//noinspection ResultOfMethodCallIgnored
					dir.mkdir();
				}
				return dir;
			}

			@Override String createFile() throws Exception{
				return saveImage( getDir() );
			}

			@Override String createDirectory() throws Exception{
				return createSubDirectory( getDir(), "test_dir" );
			}
		} );
		result.add( new FileCreator(  true,"getPrimaryStorage" ){
			String getDir(){
				return ActMain.this.getPrimaryStorage();
			}

			@Override String createFile() throws Exception{
				return saveImage( getDir() );
			}

			@Override String createDirectory() throws Exception{
				return createSubDirectory( getDir(), "test_dir" );
			}
		} );
		result.add( new FileCreator(  true,"getSecondaryStorage" ){
			String getDir(){
				return ActMain.this.getSecondaryStorage();
			}

			@Override String createFile() throws Exception{
				return saveImage( getDir() );
			}

			@Override String createDirectory() throws Exception{
				return createSubDirectory( getDir(), "test_dir" );
			}
		} );
		return result;
	}

	private Uri changeUri( String src, int mode ,boolean is_external){
		Uri uri;
		File file;
		switch( mode ){

		case 0: //File URI
			if( !is_external){
				Utils.showToast( this, true, "shall not share the file in internal storage");
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
			uri = FileProvider.getUriForFile(this, "jp.juggler.testsaf.fileprovider", file);
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
								log.d("FileProvider data: %s %s",name,value);
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

			if( !is_external){
				Utils.showToast( this, true, "shall not share the file in internal storage");
				break;
			}

			file = Utils.getFile( this, src );
			if( file == null ){
				Utils.showToast( this, true, "can't get file path from %s", src );
				break;
			}

			uri = Utils.registerMediaURI( this,file,true);
			if( uri == null ){
				Utils.showToast( this, true, "can't register media URI for %s", file.getAbsolutePath() );
				break;
			}
			return uri;
		}
		return null;
	}

}
