package jp.juggler.testsaf.utils;

import android.content.res.Resources;
import android.util.Log;

public class LogWriter{

	static final String TAG = "TestSAF";

	final String category;


	public LogWriter( String category){
		this.category = category;
	}

	@SuppressWarnings( "unused" )
	public void e( String fmt, Object... args ){
		if( args.length > 0 ) fmt = String.format( fmt, args );
		Log.e(TAG,fmt);
	}

	@SuppressWarnings( "unused" )
	public void w( String fmt, Object... args ){
		if( args.length > 0 ) fmt = String.format( fmt, args );
		Log.w(TAG,fmt);
	}

	@SuppressWarnings( "unused" )
	public void i( String fmt, Object... args ){
		if( args.length > 0 ) fmt = String.format( fmt, args );
		Log.i(TAG,fmt);
	}

	@SuppressWarnings( "unused" )
	public void v( String fmt, Object... args ){
		if( args.length > 0 ) fmt = String.format( fmt, args );
		Log.v(TAG,fmt);
	}

	@SuppressWarnings( "unused" )
	public void d( String fmt, Object... args ){
		if( args.length > 0 ) fmt = String.format( fmt, args );
		Log.d(TAG,fmt);
	}

	@SuppressWarnings( "unused" )
	public void e( Resources res,int string_id, Object... args ){
		String fmt = res.getString( string_id, args );
		Log.e(TAG,fmt);
	}

	@SuppressWarnings( "unused" )
	public void w( Resources res,int string_id, Object... args ){
		String fmt = res.getString( string_id, args );
		Log.w(TAG,fmt);
	}

	@SuppressWarnings( "unused" )
	public void i(Resources res, int string_id, Object... args ){
		String fmt = res.getString( string_id, args );
		Log.i(TAG,fmt);
	}

	@SuppressWarnings( "unused" )
	public void v( Resources res,int string_id, Object... args ){
		String fmt = res.getString( string_id, args );
		Log.v(TAG,fmt);
	}

	@SuppressWarnings( "unused" )
	public void d( Resources res,int string_id, Object... args ){
		String fmt = res.getString( string_id, args );
		Log.d(TAG,fmt);
	}

	@SuppressWarnings( "unused" )
	public static String formatError( Throwable ex, String fmt, Object... args ){
		if( args.length > 0 ) fmt = String.format( fmt, args );
		return fmt + String.format( " :%s %s", ex.getClass().getSimpleName(), ex.getMessage() );
	}

	@SuppressWarnings( "unused" )
	public static String formatError( Throwable ex, Resources res,int string_id, Object... args ){
		String fmt = res.getString( string_id, args );
		return fmt + String.format( " :%s %s", ex.getClass().getSimpleName(), ex.getMessage() );
	}

	@SuppressWarnings( "unused" )
	public void e( Throwable ex, String fmt, Object... args ){
		Log.e(TAG,formatError(ex,fmt,args));
	}

	@SuppressWarnings( "unused" )
	public void e( Throwable ex, Resources res,int string_id, Object... args ){
		Log.e(TAG,formatError(ex,res,string_id,args));
	}


}
