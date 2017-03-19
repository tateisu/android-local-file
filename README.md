# android-local-file
SAFやらFileProviderやらを使ってみたモックアップです

## ファイルのある場所

### Context#getDataDirFile

+	/data/data/{package_name}
+	/data/user/#/{package_name}
+	アプリのアンインストールで消える

### Context#getDatabasesDir

+	Context#getDataDirFile() + "/databases"
+	読み書き可能
+	setLastModified可能
+	アプリのアンインストールで消える

### Context#getFilesDir

+	getDataDirFile() + "/files"
+	読み書き可能
+	setLastModified可能
+	アプリのアンインストールで消える

### Context#getDir(name)
+	getDataDirFile() + "/app_"+name
+	読み書き可能
+	setLastModified可能
+	アプリのアンインストールで消える

### Context#getCacheDir()
+	getDataDirFile() + "/cache"
+	読み書き可能
+	setLastModified可能
+	アプリのアンインストールで消える

### Context#getExternalFilesDir()
+	/storage/emulated/{number}/Android/data/{package_name}/files/{type}
+	Primary Storage上のプライベートなフォルダ
+	読み書き可能
+	setLastModified可能
+	(4.4未満はWRITE_EXTERNAL_STORAGE権限が必要)
+	アプリのアンインストールで消える

### Context#getExternalCacheDir()
+	/storage/emulated/{number}/Android/data/{package_name}/cache
+	読み書き可能
+	setLastModified可能
+	(4.4未満はWRITE_EXTERNAL_STORAGE権限が必要)
+	アプリのアンインストールで消える

### Environment.getExternalStorageDirectory()
+	/storage/emulated/{number}/
+	WRITE_EXTERNAL_STORAGE権限があれば読み書き可能
+	setLastModifiedは不可能

### ACTION_OPEN_DOCUMENT_TREEで取得したフォルダ	
+ ACTION_OPEN_DOCUMENT_TREEが使えるのはAPI21(Android 5.0)から
+	ファイルパスへの変換は可能
+	WRITE_EXTERNAL_STORAGE権限があれば読み書き可能
+	5.0以降でもFile APIを使ってサブフォルダの作成が可能
+	setLastModifiedは不可能
+	プライマリストレージではない場合、ファイルパスへの変換は非公開APIを使えば可能

## ローカルファイルを他アプリにシェアする手順

### File URIでのシェア
+	Android 7.0以降は無条件にFileUriExposedExceptionが出る
+ Android 4.xまではMIME typeがIntentに指定されていないとビューアが選択肢に出ない

### SAFのDocument URIでのシェア
+ MIME type の指定は必須
+	SDカードなどの非プライマリストレージのdocument URIだと対応できないアプリがあるみたい？
+	MIME typeが適切ならQuickPICはプライマリストレージでもSDカードでも開けた

### FileProvider のURLを使ったシェア
+ xmlにシェア可能なpathを列挙しておく必要がある
+ 特殊な拡張子を扱うアプリの場合、FileProvider#getType() は必要に応じてオーバライドするべき
+ undocumented だが &lt;root-path /&gt; を指定するとSDカードのファイルもシェアできる

### MediaStoreに登録してからシェア
+ MediaStore.Files ならファイル種別に関わらずシェアできる
+ でもやっぱりDB登録時にMIME type の指定が必須
