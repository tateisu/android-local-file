package jp.juggler.testsaf.utils;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.storage.StorageManager;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils{

	public static void runOnMainThread( @NonNull Runnable proc ){
		if( Looper.getMainLooper().getThread() == Thread.currentThread() ){
			proc.run();
		}else{
			new Handler( Looper.getMainLooper() ).post( proc );
		}
	}

	public static void showToast( final Context context, final boolean bLong, final String fmt, final Object... args ){
		runOnMainThread( new Runnable(){
			@Override public void run(){
				Toast.makeText(
					context
					, ( args.length == 0 ? fmt : String.format( fmt, args ) )
					, bLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT
				).show();
			}
		} );
	}

	public static void showToast( final Context context, final Throwable ex, final String fmt, final Object... args ){
		runOnMainThread( new Runnable(){
			@Override public void run(){
				Toast.makeText(
					context
					, LogWriter.formatError( ex, fmt, args )
					, Toast.LENGTH_LONG
				).show();
			}
		} );
	}

	public static void showToast( final Context context, final boolean bLong, final int string_id, final Object... args ){
		runOnMainThread( new Runnable(){
			@Override public void run(){

				Toast.makeText(
					context
					, context.getString( string_id, args )
					, bLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT
				).show();
			}
		} );
	}

	public static void showToast( final Context context, final Throwable ex, final int string_id, final Object... args ){
		runOnMainThread( new Runnable(){
			@Override public void run(){
				Toast.makeText(
					context
					, LogWriter.formatError( ex, context.getResources(), string_id, args )
					, Toast.LENGTH_LONG
				).show();
			}
		} );
	}

	public static @NonNull Map<String, String> getSecondaryStorageVolumesMap( Context context ){
		Map<String, String> result = new HashMap<>();
		try{

			StorageManager sm = (StorageManager) context.getApplicationContext().getSystemService( Context.STORAGE_SERVICE );

			// SDカードスロットのある7.0端末が手元にないから検証できない
//			if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ){
//				for(StorageVolume volume : sm.getStorageVolumes() ){
//					// String path = volume.getPath();
//					String state = volume.getState();
//
//				}
//			}

			Method getVolumeList = sm.getClass().getMethod( "getVolumeList" );
			Object[] volumes = (Object[]) getVolumeList.invoke( sm );
			//
			for( Object volume : volumes ){
				Class<?> volume_clazz = volume.getClass();

				String path = (String) volume_clazz.getMethod( "getPath" ).invoke( volume );
				String state = (String) volume_clazz.getMethod( "getState" ).invoke( volume );
				if( ! TextUtils.isEmpty( path ) && "mounted".equals( state ) ){
					//
					boolean isPrimary = (Boolean) volume_clazz.getMethod( "isPrimary" ).invoke( volume );
					if( isPrimary ) result.put( "primary", path );
					//
					String uuid = (String) volume_clazz.getMethod( "getUuid" ).invoke( volume );
					if( ! TextUtils.isEmpty( uuid ) ) result.put( uuid, path );
				}
			}
		}catch( Throwable ex ){
			ex.printStackTrace();
		}
		return result;
	}

	public static String toCamelCase( String src ){
		StringBuilder sb = new StringBuilder();
		for( String s : src.split( "_" ) ){
			if( TextUtils.isEmpty( s ) ) continue;
			sb.append( Character.toUpperCase( s.charAt( 0 ) ) );
			sb.append( s.substring( 1, s.length() ).toLowerCase() );
		}
		return sb.toString();
	}

	// 文字列とバイト列の変換
	public static @NonNull byte[] encodeUTF8( @NonNull String str ){
		try{
			return str.getBytes( "UTF-8" );
		}catch( Throwable ex ){
			return new byte[ 0 ]; // 入力がnullの場合のみ発生
		}
	}

	// 文字列とバイト列の変換
	public static @NonNull String decodeUTF8( @NonNull byte[] data ){
		try{
			return new String( data, "UTF-8" );
		}catch( Throwable ex ){
			return ""; // 入力がnullの場合のみ発生
		}
	}

	public static String getMimeType( String src ){
		String ext = MimeTypeMap.getFileExtensionFromUrl( src );
		if( ext == null ) return null;
		return MimeTypeMap.getSingleton().getMimeTypeFromExtension( ext.toLowerCase() );
	}

	public static boolean isExternalStorageDocument( Uri uri ){
		return "com.android.externalstorage.documents".equals( uri.getAuthority() );
	}

	private static final String PATH_TREE = "tree";
	private static final String PATH_DOCUMENT = "document";

	public static String getDocumentId( Uri documentUri ){
		final List<String> paths = documentUri.getPathSegments();
		if( paths.size() >= 2 && PATH_DOCUMENT.equals( paths.get( 0 ) ) ){
			// document
			return paths.get( 1 );
		}
		if( paths.size() >= 4 && PATH_TREE.equals( paths.get( 0 ) )
			&& PATH_DOCUMENT.equals( paths.get( 2 ) ) ){
			// document in tree
			return paths.get( 3 );
		}
		if( paths.size() >= 2 && PATH_TREE.equals( paths.get( 0 ) ) ){
			// tree
			return paths.get( 1 );
		}
		throw new IllegalArgumentException( "Invalid URI: " + documentUri );
	}

	public static @Nullable File getFile( Context context, @NonNull String path ){
		try{
			if( path.startsWith( "/" ) ) return new File( path );
			Uri uri = Uri.parse( path );
			if( "file".equals( uri.getScheme() ) ) return new File( uri.getPath() );

			if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ){
				if( isExternalStorageDocument( uri ) ){
					try{
						final String docId = getDocumentId( uri );
						final String[] split = docId.split( ":" );
						if( split.length >= 2 ){
							final String uuid = split[ 0 ];
							if( "primary".equalsIgnoreCase( uuid ) ){
								return new File( Environment.getExternalStorageDirectory() + "/" + split[ 1 ] );
							}else{
								Map<String, String> volume_map = Utils.getSecondaryStorageVolumesMap( context );
								String volume_path = volume_map.get( uuid );
								if( volume_path != null ){
									return new File( volume_path + "/" + split[ 1 ] );
								}
							}
						}
					}catch( Throwable ex2 ){
						ex2.printStackTrace();
					}
				}
			}
			// MediaStore Uri
			Cursor cursor = context.getContentResolver().query( uri, null, null, null, null );
			if( cursor != null ){
				try{
					if( cursor.moveToFirst() ){
						int col_count = cursor.getColumnCount();
						for( int i = 0 ; i < col_count ; ++ i ){
							int type = cursor.getType( i );
							if( type != Cursor.FIELD_TYPE_STRING ) continue;
							String name = cursor.getColumnName( i );
							String value = cursor.isNull( i ) ? null : cursor.getString( i );
							if( ! TextUtils.isEmpty( value ) ){
								if( "filePath".equals( name ) ){
									return new File( value );
								}
							}
						}
					}
				}finally{
					cursor.close();
				}
			}
		}catch( Throwable ex ){
			ex.printStackTrace();
		}
		return null;
	}

	public static Uri registerMediaURI( Context context, @NonNull File src, boolean is_external ){
		// 既に登録済みかも
		Uri files_uri = MediaStore.Files.getContentUri( is_external ? "external" : "internal" );
		Cursor cursor = context.getContentResolver().query(
			files_uri
			, null
			, MediaStore.Files.FileColumns.DATA + "=?"
			, new String[]{ src.getAbsolutePath() }
			, null
		);
		if( cursor != null ){
			try{
				if( cursor.moveToFirst() ){
					int colidx_id = cursor.getColumnIndex( BaseColumns._ID );
					long id = cursor.getLong( colidx_id );
					return Uri.parse( files_uri.toString() + "/" + id );
				}

			}finally{
				cursor.close();
			}
		}

		// 登録する
		ContentValues cv = new ContentValues();
		String name = src.getName();
		String mime_type = Utils.getMimeType( name );
		cv.put( MediaStore.Files.FileColumns.DATA, src.getAbsolutePath() );
		cv.put( MediaStore.Files.FileColumns.DISPLAY_NAME, name );
		cv.put( MediaStore.Files.FileColumns.TITLE, name );
		cv.put( MediaStore.Files.FileColumns.MIME_TYPE, mime_type );
		return context.getContentResolver().insert( files_uri, cv );
	}
}
