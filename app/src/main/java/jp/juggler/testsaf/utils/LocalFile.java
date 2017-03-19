package jp.juggler.testsaf.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.provider.DocumentFile;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import jp.juggler.testsaf.ActMain;
import jp.juggler.testsaf.FolderPicker;
import jp.juggler.testsaf.Pref;

/*

 端末上にあるかもしれないファイルの抽象化

 機能1
 OSバージョンによってFileとDocumentFileを使い分ける

 機能2
 転送対象ファイルが存在しないフォルダを作成したくない
 prepareFile()した時点で親フォルダまで遡って作成したい
 しかし DocumentFile だと作成する前のフォルダを表現できない
 親フォルダがまだ作成されてなくても「親フォルダ＋名前」の形式でファイルパスを保持する

*/

public class LocalFile{

	static final LogWriter log = new LogWriter( "LocalFile" );

	public static final int DOCUMENT_FILE_VERSION = 21;

	private Object local_file;

	public LocalFile( Context context, String folder_uri ){
		if( Build.VERSION.SDK_INT >= DOCUMENT_FILE_VERSION ){
			local_file = DocumentFile.fromTreeUri( context, Uri.parse( folder_uri ) );
		}else{
			local_file = new File( folder_uri );
		}
	}

	private LocalFile parent;
	private String name;

	public LocalFile( LocalFile parent, String name ){
		this.parent = parent;
		this.name = name;
	}

	public LocalFile getParent(){
		return parent;
	}

	public String getName(){
		return name;
	}

	// local_fileで表現されたフォルダ中に含まれるエントリの一覧
	// 適当にキャッシュする
	private ArrayList<Object> child_list;

	// エントリを探索
	private Object findChild( boolean bCreate, String target_name ){
		if( prepareFileList( bCreate ) ){
			int start = 0;
			int end = child_list.size();
			while( ( end - start ) > 0 ){
				int mid = ( ( start + end ) >> 1 );
				Object x = child_list.get( mid );
				int i;
				if( Build.VERSION.SDK_INT >= DOCUMENT_FILE_VERSION ){
					i = target_name.compareTo( ( (DocumentFile) x ).getName() );
				}else{
					i = target_name.compareTo( ( (File) x ).getName() );
				}
				if( i < 0 ){
					end = mid;
				}else if( i > 0 ){
					start = mid + 1;
				}else{
					return x;
				}
			}
		}
		return null;
	}

	private boolean prepareFileList( boolean bCreate ){
		if( child_list == null ){
			if( local_file == null && parent != null ){
				if( parent.prepareDirectory( bCreate ) ){
					local_file = parent.findChild( bCreate, name );
				}
			}
			if( local_file != null ){
				try{
					ArrayList<Object> result = new ArrayList<>();
					if( Build.VERSION.SDK_INT >= DOCUMENT_FILE_VERSION ){
						Collections.addAll( result, ( (DocumentFile) local_file ).listFiles() );
						Collections.sort( result, new Comparator<Object>(){
							@Override public int compare( Object a, Object b ){
								return ( (DocumentFile) a ).getName().compareTo( ( (DocumentFile) b ).getName() );
							}
						} );
					}else{
						Collections.addAll( result, ( (File) local_file ).listFiles() );
						Collections.sort( result, new Comparator<Object>(){
							@Override public int compare( Object a, Object b ){
								return ( (File) a ).getName().compareTo( ( (File) b ).getName() );
							}
						} );
					}
					child_list = result;
				}catch( Throwable ex ){
					ex.printStackTrace();
					log.e( ex, "listFiles() failed." );
				}
			}
		}
		return child_list != null;
	}

	private boolean prepareDirectory( boolean bCreate ){
		try{
			if( local_file == null && parent != null ){
				if( parent.prepareDirectory( bCreate ) ){
					local_file = parent.findChild( bCreate, name );
					if( local_file == null && bCreate ){
						if( Build.VERSION.SDK_INT >= DOCUMENT_FILE_VERSION ){
							local_file = ( (DocumentFile) parent.local_file ).createDirectory( name );
						}else{
							local_file = new File( (File) parent.local_file, name );
							if( ! ( (File) local_file ).mkdir() ){
								local_file = null;
							}
						}
						if( local_file == null ){
							log.e( "folder creation failed." );
						}
					}
				}
			}
		}catch( Throwable ex ){
			log.e( ex, "folder creation failed." );
		}
		return local_file != null;
	}

	@SuppressWarnings( "BooleanMethodIsAlwaysInverted" )
	public boolean prepareFile( boolean bCreate ){
		try{
			if( local_file == null && parent != null ){
				if( parent.prepareDirectory( bCreate ) ){
					local_file = parent.findChild( bCreate, name );
					if( local_file == null && bCreate ){
						if( Build.VERSION.SDK_INT >= DOCUMENT_FILE_VERSION ){
							local_file = ( (DocumentFile) parent.local_file ).createFile( "application/octet-stream", name );
						}else{
							local_file = new File( (File) parent.local_file, name );
						}
						if( local_file == null ){
							log.e( "file creation failed." );
						}
					}
				}
			}

		}catch( Throwable ex ){
			log.e( ex, "file creation failed." );
		}
		return local_file != null;
	}

	public long length( boolean bCreate ){
		if( prepareFile( bCreate ) ){
			if( Build.VERSION.SDK_INT >= DOCUMENT_FILE_VERSION ){
				return ( (DocumentFile) local_file ).length();
			}else{
				return ( (File) local_file ).length();
			}
		}
		return 0L;
	}

	public OutputStream openOutputStream( Context context ) throws FileNotFoundException{
		if( Build.VERSION.SDK_INT >= DOCUMENT_FILE_VERSION ){
			Uri file_uri = ( (DocumentFile) local_file ).getUri();
			return context.getContentResolver().openOutputStream( file_uri );
		}else{
			return new FileOutputStream( ( (File) local_file ) );
		}
	}

	public InputStream openInputStream( Context context ) throws FileNotFoundException{
		if( Build.VERSION.SDK_INT >= DOCUMENT_FILE_VERSION ){
			Uri file_uri = ( (DocumentFile) local_file ).getUri();
			return context.getContentResolver().openInputStream( file_uri );
		}else{
			return new FileInputStream( ( (File) local_file ) );
		}
	}

	public boolean delete(){
		if( Build.VERSION.SDK_INT >= DOCUMENT_FILE_VERSION ){
			return ( (DocumentFile) local_file ).delete();
		}else{
			return ( (File) local_file ).delete();
		}
	}

	public boolean renameTo( String name ){
		if( Build.VERSION.SDK_INT >= DOCUMENT_FILE_VERSION ){
			return ( (DocumentFile) local_file ).renameTo( name );
		}else{
			return ( (File) local_file ).renameTo(
				new File( ( (File) local_file ).getParentFile(), name )
			);
		}
	}

	public String getFileUri( boolean bCreate ){
		if( ! prepareFile( bCreate ) ) return null;
		if( Build.VERSION.SDK_INT >= DOCUMENT_FILE_VERSION ){
			return ( (DocumentFile) local_file ).getUri().toString();
		}else{
			return ( (File) local_file ).getAbsolutePath();
		}
	}




	public void setFileTime( Context context, long time ){
		try{
			String uri_or_path = getFileUri( false );
			if( uri_or_path == null ) return;

			File path = Utils.getFile( context,uri_or_path );
			if( path == null ) return;

			if( path.isFile() ) path.setLastModified( time );
		}catch( Throwable ex ){
			log.e( "setLastModified() failed." );
		}
	}

	@SuppressLint( "NewApi" )
	public static String handleFolderPickerResult( Context context, int resultCode, Intent resultData ){
		try{
			if( resultCode == Activity.RESULT_OK ){
				if( Build.VERSION.SDK_INT >= LocalFile.DOCUMENT_FILE_VERSION ){
					Uri treeUri = resultData.getData();
					// 永続的な許可を取得
					context.getContentResolver().takePersistableUriPermission(
						treeUri
						, Intent.FLAG_GRANT_READ_URI_PERMISSION
							| Intent.FLAG_GRANT_WRITE_URI_PERMISSION
					);
					return treeUri.toString();
				}else{
					String path = resultData.getStringExtra( FolderPicker.EXTRA_FOLDER );
					String error = checkFolderWritable( path );
					if( TextUtils.isEmpty( error ) ) return path;
					Utils.showToast( context, true, "folder access failed. %s", error );
				}
			}
		}catch( Throwable ex ){
			ex.printStackTrace();
			Utils.showToast( context, ex, "folder access failed." );
		}
		return null;
	}

	// pathで指定されたフォルダが実際に書き込み可能か調べる
	// 書き込み可能ならnull、そうでなければエラーを返す
	static String checkFolderWritable( String path ){
		try{
			File dir = new File( path );
			if( ! dir.mkdir() && ! dir.isDirectory() ) return "directory not exists.";
			if( ! dir.canRead() ) return "directory not readable.";
			if( ! dir.canWrite() ) return "directory not writable.";

			String name = Thread.currentThread().getId() + "." + android.os.Process.myPid();
			File test_dir = new File( dir, name );
			try{
				if( ! test_dir.mkdir() && ! test_dir.isDirectory() ) return "sub directory creation failed.";
				if( ! test_dir.canRead() ) return "directory not readable.";
				if( ! test_dir.canWrite() ) return "directory not writable.";
				File test_file = new File( test_dir, name );
				try{
					FileOutputStream fos = new FileOutputStream( test_file );
					try{
						fos.write( Utils.encodeUTF8( "TEST" ) );
					}finally{
						fos.close();
					}
				}finally{
					//noinspection ResultOfMethodCallIgnored
					test_file.delete();
				}
			}finally{
				//noinspection ResultOfMethodCallIgnored
				test_dir.delete();
			}
			return null;
		}catch( Throwable ex ){
			ex.printStackTrace();
			return LogWriter.formatError( ex, "checkFolderWritable() failed." );
		}
	}

	public static void openFolderPicker( Activity activity, int requestCode, String old_path ){
		if( Build.VERSION.SDK_INT >= LocalFile.DOCUMENT_FILE_VERSION ){
			@SuppressLint( "InlinedApi" )
			Intent intent = new Intent( Intent.ACTION_OPEN_DOCUMENT_TREE );
			activity.startActivityForResult( intent, requestCode );
		}else{
			FolderPicker.open( activity, requestCode, old_path );

		}
	}
}
