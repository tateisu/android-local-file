Context#getDataDirFile
	/data/data/{package_name}
	/data/user/#/{package_name}
	アプリのアンインストールで消える

Context#getDatabasesDir
	Context#getDataDirFile() + "/databases"
	読み書き可能
	setLastModified可能
	アプリのアンインストールで消える

Context#getFilesDir
	getDataDirFile() + "/files"
	読み書き可能
	setLastModified可能
	アプリのアンインストールで消える

Context#getDir(name)
	getDataDirFile() + "/app_"+name
	読み書き可能
	setLastModified可能
	アプリのアンインストールで消える

Context#getCacheDir()
	getDataDirFile() + "/cache"
	読み書き可能
	setLastModified可能
	アプリのアンインストールで消える

Context#getExternalFilesDir()
	/storage/emulated/{number}/Android/data/{package_name}/files/{type}
	Primary Storage上のプライベートなフォルダ
	読み書き可能
	setLastModified可能
	(4.4未満はWRITE_EXTERNAL_STORAGE権限が必要)
	アプリのアンインストールで消える

Context#getExternalCacheDir()
	/storage/emulated/{number}/Android/data/{package_name}/cache
	読み書き可能
	setLastModified可能
	(4.4未満はWRITE_EXTERNAL_STORAGE権限が必要)
	アプリのアンインストールで消える

Environment.getExternalStorageDirectory()
	/storage/emulated/{number}/
	WRITE_EXTERNAL_STORAGE権限があれば読み書き可能
	setLastModifiedは不可能

SAFのプライマリストレージ
	ACTION_OPEN_DOCUMENT_TREEが使えるのはAPI21(Android 5.0)から
	ファイルパスへの変換は可能
	WRITE_EXTERNAL_STORAGE権限があれば読み書き可能
	5.0以降でもFile APIを使ってサブフォルダの作成が可能
	setLastModifiedは不可能

SAFの非プライマリストレージ
	ACTION_OPEN_DOCUMENT_TREEが使えるのはAPI21(Android 5.0)から
	ファイルパスへの変換は非公開APIを使えば可能
	WRITE_EXTERNAL_STORAGE権限があれば読み書き可能
	5.0以降でもFile APIを使ってサブフォルダの作成が可能
	setLastModifiedは不可能

ローカルファイルを他アプリにシェアする

File URIでのシェア
	FileUriExposedExceptionが出る
	FileProviderを使うか、MediaStoreに登録してそのURIを渡す

SAFのDocument URIでのシェア
	プライマリストレージでMIME typeが適切なら開けるアプリもある
	SDカードなどの非プライマリストレージのdocument URIを開けないアプリも多い

FileProvider のURLを使ったシェア
	xmlにシェア可能なpathを列挙しておく必要がある
	特殊な拡張子を扱うアプリの場合、FileProvider#getType() は必要に応じてオーバライドするべき


