package jp.juggler.testsaf;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.widget.RemoteViews;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import it.sephiroth.android.library.exif2.ExifInterface;
import jp.juggler.testsaf.utils.LogWriter;

public class WidgetHasImage extends AppWidgetProvider{

	static final LogWriter log = new LogWriter( "WidgetHasImage" );

	// 受動的な更新
	@Override public void onUpdate( Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds ){
		if( appWidgetIds != null && appWidgetIds.length > 0 ){
			appWidgetManager.updateAppWidget( appWidgetIds, createRemoteViews( context ) );
		}
	}

	//////////////////////////////////////////////////

	// 能動的な更新
	public static void updateWidget( Context context ){
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance( context );
		ComponentName c_name = new ComponentName( context, WidgetHasImage.class );
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds( c_name );

		if( appWidgetIds != null && appWidgetIds.length > 0 ){
			appWidgetManager.updateAppWidget( appWidgetIds, createRemoteViews( context ) );
		}
	}

	// リモートビューの生成
	private static RemoteViews createRemoteViews( Context context ){
		Intent intent = new Intent( context, ActMain.class );
		intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY );
		PendingIntent pendingIntent = PendingIntent.getActivity( context, 569, intent, 0 );

		// RemoteViewsを調整
		RemoteViews views = new RemoteViews( context.getPackageName(), R.layout.widget_has_image );
		views.setOnClickPendingIntent( R.id.llWidget, pendingIntent );

		Bitmap bitmap = loadImage( context );
		if( bitmap != null) views.setImageViewBitmap( R.id.ivImage, bitmap );
		views.setTextViewText( R.id.tvText, "" );

		return views;
	}

	static final String IMAGE_FILE = "widget_image.jpg";
	static final float IMAGE_SIZE_DP = 64f;

	// リモートビュー生成時に呼ばれる
	// ファイルからBitmapを読み出す
	private static Bitmap loadImage( Context context ){
		File file = new File( context.getFilesDir(), IMAGE_FILE );
		try{
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = false;
			options.inScaled = false;

			FileInputStream is = new FileInputStream( file );
			try{
				Bitmap src = BitmapFactory.decodeStream( is, null, options );
				if( src == null ){
					log.e( "bitmap loading failed." );
				}else{
					return src;
				}
			}finally{
				try{
					is.close();
				}catch( Throwable ignored ){

				}
			}
		}catch( Throwable ex ){
			ex.printStackTrace();
			log.e( ex, "bitmap loading failed." );
		}
		return null;
	}

	public static void deleteImage( Context context ){
		File file = new File( context.getFilesDir(), IMAGE_FILE );
		file.delete();
		updateWidget( context );
	}

	private static boolean saveImage( Context context, Bitmap bitmap ){
		File file = new File( context.getFilesDir(), IMAGE_FILE );
		File file_tmp = new File( context.getFilesDir(), IMAGE_FILE + ".tmp" );
		try{
			FileOutputStream os = new FileOutputStream( file_tmp );
			try{
				bitmap.compress( Bitmap.CompressFormat.PNG, 100, os );
			}finally{
				try{
					os.close();
				}catch( Throwable ignored ){
				}
			}
			if( !file_tmp.renameTo( file ) ){
				log.e("rename failed. %s",file.getAbsolutePath() );
			}else{
				updateWidget( context );
				return true;
			}
		}catch( Throwable ex ){
			ex.printStackTrace();
			log.e( ex, "saveImage() failed." );
		}
		return false;
	}

	private static Bitmap resizeAndRotation( Bitmap src, Integer orientation, int size_max ){
		int src_w = src.getWidth();
		int src_h = src.getHeight();
		if( src_w < 1 || src_h < 1 ) return null;

		float scale;
		if( src_w >= src_h ){
			scale = size_max / (float) src_w;
		}else{
			scale = size_max / (float) src_h;
		}
		int after_w = src_w;
		int after_h = src_h;

		Matrix matrix = new Matrix();
		matrix.reset();
		// 画像の中心が原点に来るようにして
		matrix.postTranslate( src_w * - 0.5f, src_h * - 0.5f );
		// スケーリング
		matrix.postScale( scale, scale );

		// 回転情報があれば回転
		if( orientation != null ){
			int tmp;
			switch( orientation.shortValue() ){
			default:
				break;
			case 2: // 上限反転
				matrix.postScale( 1f, - 1f );
				break;
			case 3: // 180度回転
				matrix.postRotate( 180f );
				break;
			case 4: // 左右反転
				matrix.postScale( - 1f, 1f );
				break;
			case 5: // 左右反転して90度回転
				matrix.postScale( 1f, - 1f );
				matrix.postRotate( - 90f );
				tmp = after_w;
				after_w = after_h;
				after_h = tmp;
				break;
			case 6: // 90度回転
				matrix.postRotate( 90f );
				tmp = after_w;
				after_w = after_h;
				after_h = tmp;
				break;
			case 7: // 左右反転して90度回転
				matrix.postScale( 1f, - 1f );
				matrix.postRotate( 90f );
				tmp = after_w;
				after_w = after_h;
				after_h = tmp;
				break;
			case 8: // 90度回転
				matrix.postRotate( - 90f );
				tmp = after_w;
				after_w = after_h;
				after_h = tmp;
				break;
			}
		}
		// 表示領域に埋まるように平行移動
		matrix.postTranslate( after_w * 0.5f, after_h * 0.5f );

		Bitmap dst = null;
		try{
			dst = Bitmap.createBitmap( after_w, after_h, Bitmap.Config.ARGB_8888 );
			Canvas canvas = new Canvas( dst );
			Paint paint = new Paint();
			paint.setFilterBitmap( true );
			canvas.drawBitmap( src, matrix, paint );
			return dst;
		}catch( Throwable ex ){
			ex.printStackTrace();
			log.e( ex, "bitmap resize failed." );
			if( dst != null ) dst.recycle();
		}
		return null;
	}

	public static boolean updateImage( Context context, File file ){

		final float density = context.getResources().getDisplayMetrics().density;
		final int size_max = (int) ( 0.5f + density * IMAGE_SIZE_DP );

		Integer orientation = null;
		try{
			ExifInterface exif = new ExifInterface();
			exif.readExif( file.getAbsolutePath(), ExifInterface.Options.OPTION_ALL );
			orientation = exif.getTagIntValue( ExifInterface.TAG_ORIENTATION );

			Bitmap src = exif.getThumbnailBitmap();
			if( src != null ){
				try{
					Bitmap resized = resizeAndRotation( src, orientation, size_max );
					if( resized != null ){
						try{
							return saveImage( context, resized );
						}finally{
							resized.recycle();
						}
					}
				}finally{
					src.recycle();
				}
			}
		}catch( Throwable ex ){
			ex.printStackTrace();
			log.e( ex, "loading exif failed." );
		}

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		try{
			FileInputStream is = new FileInputStream( file );
			try{
				BitmapFactory.decodeStream( is, null, options );
			}finally{
				try{
					is.close();
				}catch( Throwable ignored ){

				}
			}
		}catch( Throwable ex ){
			ex.printStackTrace();
			log.e( ex, "can not get image bounds." );
			return false;
		}

		int src_w = options.outWidth;
		int src_h = options.outHeight;
		if( src_w < 1 || src_h < 1 ){
			log.e( "too small image." );
			return false;
		}

		int scale_bits = 0;
		int scaled_w = src_w;
		int scaled_h = src_h;
		while( scaled_w >= size_max * 2 || scaled_h >= size_max * 2 ){
			++ scale_bits;
			scaled_w >>= 1;
			scaled_h >>= 1;
		}
		options.inJustDecodeBounds = false;
		options.inSampleSize = ( 1 << scale_bits );
		options.inScaled = false;
		try{
			FileInputStream is = new FileInputStream( file );
			try{
				Bitmap src = BitmapFactory.decodeStream( is, null, options );
				if( src == null ){
					log.e( "bitmap loading failed." );
				}else{
					try{
						Bitmap resized = resizeAndRotation( src, orientation, size_max );
						if( resized != null ){
							try{
								return saveImage( context, resized );
							}finally{
								resized.recycle();
							}
						}
					}finally{
						src.recycle();
					}
				}
			}finally{
				try{
					is.close();
				}catch( Throwable ignored ){
				}
			}
		}catch( Throwable ex ){
			ex.printStackTrace();
			log.e( ex, "bitmap loading failed." );
		}
		return false;
	}
}
