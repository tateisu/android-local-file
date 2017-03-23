package jp.juggler.testsaf.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.provider.DocumentFile;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import jp.juggler.testsaf.Pref;

abstract public class FileCreator{

	public boolean is_external;
	public final String name;

	FileCreator( boolean is_external, String name ){
		this.is_external = is_external;
		this.name = name;
	}

	public abstract String createFile() throws Exception;

	public abstract String createDirectory() throws Exception;


	public static String getPrimaryStorage(Context context){
		SharedPreferences pref = Pref.pref( context );
		return pref.getString( Pref.UI_PRIMARY_STORAGE, "" );
	}

	public static String getSecondaryStorage(Context context){
		SharedPreferences pref = Pref.pref( context );
		return pref.getString( Pref.UI_SECONDARY_STORAGE, "" );
	}

	public static String saveImage( Context context,File dir ) throws Exception{
		File file = new File( dir, "image1.jpg" );

		InputStream is = context.getAssets().open( "image1.jpg" );
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

	public static String saveImage(  Context context,DocumentFile dir ) throws Exception{
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

		InputStream is = context.getAssets().open( "image1.jpg" );
		try{
			OutputStream os = context.getContentResolver().openOutputStream( file_uri );
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

	public static String saveImage( Context context,String dir ) throws Exception{

		if( dir.startsWith( "/" ) ) return saveImage( context,new File( dir ) );

		Uri uri = Uri.parse( dir );

		if( uri.getScheme().equals( "file" ) ) return saveImage( context,new File( uri.getPath() ) );

		DocumentFile df = DocumentFile.fromTreeUri( context, uri );
		return saveImage( context,df );
	}

	public static String createSubDirectory( File parent, String name ) throws Exception{
		File dir = new File( parent, name );
		if( ! dir.mkdir() && ! dir.isDirectory() ){
			throw new RuntimeException( String.format( "directory creation failed. %s", dir.getAbsolutePath() ) );
		}
		return dir.getAbsolutePath();
	}

	public static String createSubDirectory(  Context context, String parent, String name ) throws Exception{

		if( parent.startsWith( "/" ) ) return createSubDirectory( new File( parent ), name );

		Uri uri = Uri.parse( parent );
		if( uri.getScheme().equals( "file" ) ) return createSubDirectory( new File( uri.getPath() ), name );

		File path = Utils.getFile( context, parent );
		if( path == null ){
			throw new RuntimeException( String.format( "can not get path from uri. %s", parent ) );
		}
		return createSubDirectory( path, name );
	}

	public static ArrayList<FileCreator> getFileCreatorList( final Context context ){
		ArrayList<FileCreator> result = new ArrayList<>();
		result.add(
			new FileCreator( false, "Context#getFilesDir" ){
				File getDir(){
					File dir = context.getFilesDir();
					if( dir != null ){
						//noinspection ResultOfMethodCallIgnored
						dir.mkdir();
					}
					return dir;
				}

				@Override public String createFile() throws Exception{
					return saveImage( context, getDir() );
				}

				@Override public String createDirectory() throws Exception{
					return createSubDirectory( getDir(), "test_dir" );
				}
			} );

		result.add( new FileCreator( false, "Context#getCacheDir" ){
			File getDir(){
				File dir = context.getCacheDir();
				if( dir != null ){
					//noinspection ResultOfMethodCallIgnored
					dir.mkdir();
				}
				return dir;
			}

			@Override public String createFile() throws Exception{
				return saveImage( context, getDir() );
			}

			@Override public String createDirectory() throws Exception{
				return createSubDirectory( getDir(), "test_dir" );
			}
		} );
		result.add( new FileCreator( true, "Context#getExternalFilesDir" ){
			File getDir(){
				File dir =  context.getExternalFilesDir( Environment.DIRECTORY_DOWNLOADS );
				if( dir != null ){
					//noinspection ResultOfMethodCallIgnored
					dir.mkdir();
				}
				return dir;
			}

			@Override public String createFile() throws Exception{
				return saveImage(  context, getDir() );
			}

			@Override public String createDirectory() throws Exception{
				return createSubDirectory(  getDir(), "test_dir" );
			}
		} );
		result.add( new FileCreator( true, "Context#getExternalCacheDir" ){
			File getDir(){
				File dir = context.getExternalCacheDir();
				if( dir != null ){
					//noinspection ResultOfMethodCallIgnored
					dir.mkdir();
				}
				return dir;
			}

			@Override public String createFile() throws Exception{
				return saveImage( context,getDir() );
			}

			@Override public String createDirectory() throws Exception{
				return createSubDirectory( getDir(), "test_dir" );
			}

		} );
		result.add( new FileCreator( true, "Environment#getExternalStorageDirectory" ){
			File getDir(){
				File dir = Environment.getExternalStorageDirectory();
				if( dir != null ){
					//noinspection ResultOfMethodCallIgnored
					dir.mkdir();
				}
				return dir;
			}

			@Override public String createFile() throws Exception{
				return saveImage( context,getDir() );
			}

			@Override public String createDirectory() throws Exception{
				return createSubDirectory( getDir(), "test_dir" );
			}
		} );
		result.add( new FileCreator( true, "getPrimaryStorage" ){
			String getDir(){
				return getPrimaryStorage(context);
			}

			@Override public String createFile() throws Exception{
				return saveImage( context,getDir() );
			}

			@Override public String createDirectory() throws Exception{
				return createSubDirectory( context,getDir(), "test_dir" );
			}
		} );
		result.add( new FileCreator( true, "getSecondaryStorage" ){
			String getDir(){
				return getSecondaryStorage(context);
			}

			@Override public String createFile() throws Exception{
				return saveImage( context,getDir() );
			}

			@Override public String createDirectory() throws Exception{
				return createSubDirectory( context,getDir(), "test_dir" );
			}
		} );
		return result;
	}
}
