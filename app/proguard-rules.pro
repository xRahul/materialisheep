# App-specific R8 rules. Component, view and preference classes referenced from
# XML are kept by the AAPT-generated rules; Dagger, Retrofit, OkHttp and RxJava
# ship their own consumer rules.

# Gson maps JSON to these fields by name through reflection. The JSON fields are
# annotated @Keep, which androidx.annotation turns into a member keep; the class
# rules below also cover the Algolia response wrappers.
-keepclassmembers class in.rahulja.materialhn.data.HackerNewsItem {
    <fields>;
}
-keepclassmembers class in.rahulja.materialhn.data.AlgoliaClient$* {
    <fields>;
}

# Called from the PDF.js viewer over addJavascriptInterface.
-keepclassmembers class in.rahulja.materialhn.WebFragment$PdfAndroidJavascriptBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Parcelable CREATOR fields are resolved reflectively by the framework.
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
