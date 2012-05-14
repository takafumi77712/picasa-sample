package stndstn.picasatest;

//import android.R;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.android.*; 

public class PicasaTestActivity extends Activity {
    private static final String TAG = "IntentTestActivity";
	static final String PREF = "stndstn.timershot.pref";
    PicasaTool mPicasa = new PicasaTool();
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(stndstn.intenttest.R.layout.main);
        
        ImageButton btn = (ImageButton) this.findViewById(stndstn.intenttest.R.id.imageButton1);
        btn.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
		    	Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		    	startActivityForResult(intent, 0);
			}
		});
	    mPicasa.init(this);
    }
    
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
            	//If the EXTRA_OUTPUT is not present, then a small sized image is returned as a Bitmap object in the extra field. 
            	Bundle b = data.getExtras();
                ImageView img = (ImageView)this.findViewById(stndstn.intenttest.R.id.imageView1);
                Set<String> keys = b.keySet();
                Iterator<String> k = keys.iterator();
                while(k.hasNext()) {
                	Object o = b.get(k.next());
                	if(o.getClass() == Bitmap.class) {
                    	Bitmap bmp = (Bitmap)o;
                    	//img.setImageBitmap(bmp);  
                    	//
                        FileOutputStream os = null;
                        String name = "pict.jpg";
                	    try {
                	        os = this.openFileOutput(name, Context.MODE_WORLD_WRITEABLE);
                        	bmp.compress(Bitmap.CompressFormat.JPEG, 100, os);
                	        os.close();	        
            		        String path = getFileStreamPath(name).getPath();
                    		Log.i(TAG, "path= " + path);
                    	    File f = new File(path);
                    	    long len = f.length();
                    	    Uri uriS = Uri.fromFile(f);
                    	    StringBuffer href = new StringBuffer();
                    	    if(mPicasa.sendData(uriS, name, len, href)) {	            	    	
        	            		Log.i(TAG, "href= " + href);		
                    	    }
                        	img.setImageURI(Uri.parse(path));
                	    } catch (IOException e) {
                	        // Unable to create file, likely because external storage is
                	        // not currently mounted.
                	        Log.w(TAG, "createContextJpegFile Error writing " + name, e);
                	    }
                    	break;
                	}                				
                }
            }
        }
        else {
        	mPicasa.onActivityResult(requestCode, resultCode, data);
        }
    }
}