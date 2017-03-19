package jp.juggler.testsaf;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import jp.juggler.testsaf.utils.LocalFile;
import jp.juggler.testsaf.utils.LogWriter;
import jp.juggler.testsaf.utils.Utils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static jp.juggler.testsaf.RegexMatcher.matchesRegex;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

@RunWith( AndroidJUnit4.class )
@LargeTest
public class ActMainTest{

	static final LogWriter log = new LogWriter( "ActMainTest" );
	private static final String TAG = "ActMainTest";
	private static final String TEST_STRING = "Espresso";

	@Rule
	public ActivityTestRule<ActMain> mActivityRule = new ActivityTestRule<>( ActMain.class );

	private ActMain mActMain;
	private String packageName;

	@Before
	public void setUp() throws Exception{
		mActMain = mActivityRule.getActivity();
		packageName = mActMain.getPackageName();
	}

	@After
	public void tearDown() throws Exception{
	}

//	@Test
//	public void changeText_sameActivity(){
//		mActMain = mActivityRule.getActivity();
//
//		Log.d(TAG,"changeText_sameActivity. activity="+mActMain);
//
//		onView(withId(R.id.editTextUserInput))
//			.perform( typeText(TEST_STRING) ,closeSoftKeyboard() )
//			;
//		onView(withId(R.id.btnTestSample1))
//			.perform( click() )
//			;
//
//		onView(withId(R.id.btnTestSample1))
//			.check(matches(withText(TEST_STRING)));
//	}

	@Test
	public void Context_getFilesDir(){
		assertThat(
			mActMain.getFilesDir().getAbsolutePath()
			, matchesRegex( "/data/(data|user/\\d+)/\\Q" + packageName + "\\E/files" )
		);
	}

	@Test
	public void Context_getDir(){
		mActMain = mActivityRule.getActivity();
		String packageName = mActMain.getPackageName();
		String name = "test";
		assertThat(
			mActMain.getDir( name, Context.MODE_PRIVATE ).getAbsolutePath()
			, matchesRegex( "/data/(data|user/\\d+)/\\Q" + packageName + "\\E/app_" + name )
		);
	}

	@Test
	public void Context_getCacheDir(){
		mActMain = mActivityRule.getActivity();
		String packageName = mActMain.getPackageName();
		assertThat(
			mActMain.getCacheDir().getAbsolutePath()
			, matchesRegex( "/data/(data|user/\\d+)/\\Q" + packageName + "\\E/cache" )
		);
	}

	// アプリ固有フォルダへの書き込みは権限がなくてもできる
	@Test
	public void Context_getFilesDir_write_read() throws Exception{
		String name = "test";
		String encoding = "UTF-8";
		String content = "dummy";
		//
		FileOutputStream fos = mActMain.openFileOutput( name, Context.MODE_PRIVATE );
		fos.write( content.getBytes( encoding ) );
		fos.close();
		//
		FileInputStream fis = mActMain.openFileInput( name );
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		IOUtils.copy( fis, bao );
		String a = new String( bao.toByteArray(), encoding );

		assertThat(
			a
			, is( content )
		);
	}

	// アプリ固有フォルダへの書き込みは権限がなくてもできる
	@Test
	public void Context_getCacheDir_write_read() throws Exception{
		String name = "test";
		String encoding = "UTF-8";
		String content = "dummy";
		//
		File dir = mActMain.getCacheDir();
		File tmp_file = File.createTempFile( name, "txt", dir );
		//
		FileOutputStream fos = new FileOutputStream( tmp_file );
		fos.write( content.getBytes( encoding ) );
		fos.close();
		//
		FileInputStream fis = new FileInputStream( tmp_file );
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		IOUtils.copy( fis, bao );
		String a = new String( bao.toByteArray(), encoding );

		assertThat(
			a
			, is( content )
		);
	}

	// プライマリストレージ上のアプリ固有フォルダへの書き込み
	// 4.4以降は権限がなくてもよい
	// それより前は WRITE_EXTERNAL_STORAGE がないと書けない
	@Test
	public void Context_getExternalFilesDir_write_read() throws Exception{
		String name = "test.txt";
		String encoding = "UTF-8";
		String content = "dummy";

		File dir = mActMain.getExternalFilesDir( Environment.DIRECTORY_DOWNLOADS );
		if( dir == null ){
			fail( "getExternalFilesDir() returns null." );
			return;
		}

		if( Build.VERSION.SDK_INT >= 21 ){
			String state = Environment.getExternalStorageState( dir );
			if( ! state.equals( Environment.MEDIA_MOUNTED ) ){
				fail( "getExternalFilesDir() not mounted." );
				return;
			}
			boolean is_emulated = Environment.isExternalStorageEmulated( dir );
			Log.d( TAG, "isExternalStorageEmulated()=" + is_emulated );
		}

		if( ! dir.mkdir() && ! dir.isDirectory() ){
			fail( "getExternalFilesDir() mkdir failed." );
			return;
		}

		if( ! dir.canRead() ){
			fail( "getExternalFilesDir() canRead() failed." );
			return;
		}
		if( ! dir.canWrite() ){
			fail( "getExternalFilesDir() canWrite() failed." );
			return;
		}

		File tmp_file = new File( dir, name );

		//
		FileOutputStream fos = new FileOutputStream( tmp_file );
		fos.write( content.getBytes( encoding ) );
		fos.close();
		//
		FileInputStream fis = new FileInputStream( tmp_file );
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		IOUtils.copy( fis, bao );
		String a = new String( bao.toByteArray(), encoding );

		assertThat( a, is( content ) );
	}

	// プライマリストレージ上の共用フォルダへの書き込み
	// WRITE_EXTERNAL_STORAGE があれば書けるんだっけ？
	@Test
	public void Environment_getExternalStorageDirectory_write_read() throws Exception{
		String name = "test.txt";
		String encoding = "UTF-8";
		String content = "dummy";

		File dir = Environment.getExternalStorageDirectory();
		if( dir == null ){
			fail( "getExternalStorageDirectory() returns null." );
			return;
		}

		if( Build.VERSION.SDK_INT >= 21 ){
			String state = Environment.getExternalStorageState( dir );
			if( ! state.equals( Environment.MEDIA_MOUNTED ) ){
				fail( "getExternalStorageDirectory() not mounted." );
				return;
			}
			boolean is_emulated = Environment.isExternalStorageEmulated( dir );
			Log.d( TAG, "isExternalStorageEmulated()=" + is_emulated );
		}

		if( ! dir.mkdir() && ! dir.isDirectory() ){
			fail( "getExternalStorageDirectory() mkdir failed." );
			return;
		}

		if( ! dir.canRead() ){
			// 7.1.1で WRITE_EXTERNAL_STORAGE なしだとここを通る
			fail( "getExternalStorageDirectory() canRead() failed." );
			return;
		}
		if( ! dir.canWrite() ){
			fail( "getExternalStorageDirectory() canWrite() failed." );
			return;
		}

		File tmp_file = new File( dir, name );

		//
		FileOutputStream fos = new FileOutputStream( tmp_file );
		fos.write( content.getBytes( encoding ) );
		fos.close();
		//
		FileInputStream fis = new FileInputStream( tmp_file );
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		IOUtils.copy( fis, bao );
		String a = new String( bao.toByteArray(), encoding );

		assertThat( a, is( content ) );
	}



	private boolean readTest( InputStream is ) throws Exception{
		if( is == null ) throw new NullPointerException( "InputStream is null" );
		try{
			ByteArrayOutputStream bao = new ByteArrayOutputStream();
			IOUtils.copy( is, bao );
			return true;
		}finally{
			try{
				is.close();
			}catch( Throwable ignored ){
			}
		}
	}

	private boolean readTest( File file ) throws Exception{
		return readTest( new FileInputStream( file ) );
	}

	private boolean readTest( String file ) throws Exception{
		log.d( "readTest %s", file );

		if( file.startsWith( "/" ) ){
			return readTest( new File( file ) );
		}

		Uri uri = Uri.parse( file );
		if( "file".equals( uri.getScheme() ) ){
			return readTest( new File( uri.getPath() ) );
		}

		DocumentFile df = DocumentFile.fromSingleUri( mActMain, uri );
		boolean rv = readTest( mActMain.getContentResolver().openInputStream( uri ) );

		File path = Utils.getFile( mActMain, uri.toString() );
		if( path != null ){
			log.d( "readTest %s", path.getAbsolutePath() );
			rv = ( rv && readTest( path ) );
		}
		return rv;
	}

	private boolean setTime( File file, long t ){
		if( file.setLastModified( t ) ) return true;
		log.e( "setLastModified failed. %s", file.getAbsolutePath() );
		return false;
	}

	private boolean setTime( String file, long t ){
		if( file.startsWith( "/" ) ){
			return setTime( new File( file ), t );
		}

		Uri uri = Uri.parse( file );
		if( "file".equals( uri.getScheme() ) ){
			return setTime( new File( uri.getPath() ), t );
		}

		File path = Utils.getFile( mActMain, uri.toString() );
		if( path == null ){
			log.e( "can not get real file path for uri %s", file );
			return false;
		}
		return setTime( path, t );
	}

	// プライマリストレージ上の共用フォルダへの書き込み
	// WRITE_EXTERNAL_STORAGE があれば書けるんだっけ？
	@Test
	public void createFileToSomeLocations() throws Exception{
		int error_count = 0;
		for( ActMain.FileCreator creator : mActMain.getFileCreatorList() ){

			// ファイルを作成する
			String file;

			try{
				file = creator.createFile();
				assertNotNull( file );
				log.d( "createFileToSomeLocations %s", file );
			}catch( Throwable ex ){
				ex.printStackTrace();
				log.e( ex, "file creation failed" );
				++ error_count;
				continue;
			}

			try{
				if( ! readTest( file ) ){
					++ error_count;
				}
			}catch( Throwable ex ){
				ex.printStackTrace();
				log.e( ex, "file read failed" );
				++ error_count;
				continue;
			}

			try{
				long now = System.currentTimeMillis();
				if( ! setTime( file, now ) ){
					++ error_count;
				}
			}catch( Throwable ex ){
				ex.printStackTrace();
				log.e( ex, "file read failed" );
				++ error_count;
				continue;
			}
		}
		for( ActMain.FileCreator creator : mActMain.getFileCreatorList() ){
			try{
				String sub_dir = creator.createDirectory();
				if( sub_dir == null ){
					++ error_count;
				}
				String sub_file = mActMain.saveImage( sub_dir );
				if( sub_file == null ){
					++ error_count;
				}
			}catch( Throwable ex ){
				ex.printStackTrace();
				log.e( ex, "sub dir & file creation failed." );
				++ error_count;
				continue;
			}
		}
		assertEquals( 0, error_count );
	}
}
