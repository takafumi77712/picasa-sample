package stndstn.picasatest;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.google.api.client.apache.ApacheHttpTransport;
import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.googleapis.GoogleTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.util.DateTime;
import com.google.api.client.xml.atom.AtomParser;
import com.google.api.data.picasa.v2.PicasaWebAlbums;
import com.google.api.data.picasa.v2.atom.PicasaWebAlbumsAtom;
import com.google.api.data.sample.picasa.model.AlbumEntry;
import com.google.api.data.sample.picasa.model.PicasaUrl;
import com.google.api.data.sample.picasa.model.UserFeed;

public class PicasaTool {

	  private static final String TAG = "PicasaTool";
	  private static final int REQUEST_AUTHENTICATE = 1000;
	  private static final int REQUEST_ADD_ACCOUNT = 1001;

	  private static final GoogleTransport transport =
	      new GoogleTransport();

	  private String authToken;
	  private Activity activity;
	  private boolean isAvailable = false;
	  public boolean isAvailable(){ return isAvailable; }

	  public void init(Activity a) {
		  if(isAvailable == false) {
			  this.activity = a;
			  transport.setVersionHeader(PicasaWebAlbums.VERSION);
			  transport.applicationName = "PicasaTest";
			  //transport.applicationName = "google-picasaandroidsample-1.0";
			  AtomParser parser = new AtomParser();
			  parser.namespaceDictionary = PicasaWebAlbumsAtom.NAMESPACE_DICTIONARY;
			  transport.addParser(parser);
			  HttpTransport.setLowLevelHttpTransport(ApacheHttpTransport.INSTANCE);
			  gotAccount(false);	// isAvailable must be true during this method		  
		  }
	  }

	  protected void showAccountDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle("Select a Google account");
		final AccountManager manager = AccountManager.get(activity);
		final Account[] accounts = manager.getAccountsByType("com.google");
		final int size = accounts.length;
		String[] names = new String[size];
		for (int i = 0; i < size; i++) {
		  names[i] = accounts[i].name;
		}
		// names[size] = "New Account";
		builder.setItems(names, new DialogInterface.OnClickListener() {
		  @Override
		  public void onClick(DialogInterface dialog, int which) {
		    if (which == size) {
		      addAccount(manager);
		    } else {
		    	gotAccount(manager, accounts[which]);
		    }
		  }
		});
		builder.create().show();
	  }

	  private void gotAccount(boolean tokenExpired) {
        SharedPreferences settings = activity.getSharedPreferences(PicasaTestActivity.PREF, 0);
        String accountName = settings.getString("accountName", null);
        if (accountName != null) {
          AccountManager manager = AccountManager.get(activity);
          Account[] accounts = manager.getAccountsByType("com.google");
          int size = accounts.length;
          for (int i = 0; i < size; i++) {
            Account account = accounts[i];
            if (accountName.equals(account.name)) {
              if (tokenExpired) {
                manager.invalidateAuthToken("com.google", this.authToken);
              }
              gotAccount(manager, account);
            }
          }
        }
	    showAccountDialog();
	  }

	  private void addAccount(AccountManager manager) {
	    // TODO: test!
	    try {
	      Bundle bundle =
	          manager.addAccount("google.com", PicasaWebAlbums.AUTH_TOKEN_TYPE,
	              null, null, activity, null, null).getResult();
	      if (bundle.containsKey(AccountManager.KEY_INTENT)) {
	        Intent intent = bundle.getParcelable(AccountManager.KEY_INTENT);
	        int flags = intent.getFlags();
	        flags &= ~Intent.FLAG_ACTIVITY_NEW_TASK;
	        intent.setFlags(flags);
	        activity.startActivityForResult(intent, REQUEST_ADD_ACCOUNT);
	      } else {
	        addAccountResult(bundle);
	      }
	    } catch (Exception e) {
	      handleException(e);
	      isAvailable = false;
	    }
	  }

	  private void addAccountResult(Bundle bundle) {
	    // TODO: test!
	    String authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
	    String accountName = bundle.getString(AccountManager.KEY_ACCOUNT_NAME);
	    SharedPreferences settings = activity.getSharedPreferences(PicasaTestActivity.PREF, 0);
	    SharedPreferences.Editor editor = settings.edit();
	    editor.putString("accountName", accountName);
	    editor.commit();
	    authenticatedClientLogin(authToken);
	  }

	  private void gotAccount(AccountManager manager, Account account) {
	    SharedPreferences settings = activity.getSharedPreferences(PicasaTestActivity.PREF, 0);
	    SharedPreferences.Editor editor = settings.edit();
	    editor.putString("accountName", account.name);
	    editor.commit();
	    try {
	      Bundle bundle =
	          manager.getAuthToken(account, PicasaWebAlbums.AUTH_TOKEN_TYPE, true,
	              null, null).getResult();
	      if (bundle.containsKey(AccountManager.KEY_INTENT)) {
	        Intent intent = bundle.getParcelable(AccountManager.KEY_INTENT);
	        int flags = intent.getFlags();
	        flags &= ~Intent.FLAG_ACTIVITY_NEW_TASK;
	        intent.setFlags(flags);
	        activity.startActivityForResult(intent, REQUEST_AUTHENTICATE);
	      } else if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
	        authenticatedClientLogin(bundle.getString(AccountManager.KEY_AUTHTOKEN));
	      }
	    } catch (Exception e) {
	      handleException(e);
	      isAvailable = false;
	    }
	  }

	  public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    switch (requestCode) {
	      case REQUEST_AUTHENTICATE:
	        if (resultCode == Activity.RESULT_OK) {
	          gotAccount(false);
	        } else {
	        	showAccountDialog();
	        }
	        break;
	      case REQUEST_ADD_ACCOUNT:
	        // TODO: test!
	        if (resultCode == Activity.RESULT_OK) {
	          addAccountResult(data.getExtras());
	        } else {
	        	showAccountDialog();
	        }
	    }
	  }
	  private void authenticatedClientLogin(String authToken) {
		  this.authToken = authToken;
		  transport.setClientLoginToken(authToken);
		  isAvailable = true;
	  }

	  public boolean sendData(Uri uri, String name, long len, StringBuffer out) {
		  boolean success = false;
		  for(int retry = 0; retry <=1; retry++) {
		        Log.i(TAG, "sendData [" + retry + "]");
		      try {
			        HttpRequest request = transport.buildPostRequest();
			        request.url =
			            PicasaUrl
			                .fromRelativePath("feed/api/user/default/albumid/default");
			        GoogleHeaders.setSlug(request.headers, name);
			        InputStreamContent content = new InputStreamContent();
			        content.inputStream =
			            activity.getContentResolver().openInputStream(uri);
			        content.type = "image/jpeg";
			        content.length = len;
			        request.content = content;
			        HttpResponse res = request.execute();
			        Log.i(TAG, "sendData HttpResponse statusCode " + res.statusCode);
			        if(res.statusCode == 201) {
				        success = true;
				        if(out != null) {
					        try {
								DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
								InputSource is = new InputSource(res.getContent());
								Document d = db.parse(is);
								NodeList links = d.getElementsByTagName("link");
								for(int ii = 0; ii < links.getLength(); ii++) {
									Node n = links.item(ii);
									NamedNodeMap attrs = n.getAttributes();
									Node rel = attrs.getNamedItem("rel");
									if(rel.getNodeValue().contains("#canonical")) {
										Node href = attrs.getNamedItem("href");
								        Log.i(TAG, "sendData HttpResponse link " + href.getNodeValue());
								    	out.delete(0, out.length());
								    	out.append(href.getNodeValue());
								        break;
									}
								}
							} catch (ParserConfigurationException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (FactoryConfigurationError e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (SAXException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}	        
				        }
			        }//201
			        res.ignore();
			        if(success) break;
			  } catch (IOException e) {
				  handleException(e);
			  }
		  }//retry
		  return success;
	  }
	  private void handleException(Exception e) {
	    e.printStackTrace();
	    if (e instanceof HttpResponseException) {
	      int statusCode = ((HttpResponseException) e).response.statusCode;
	      Log.i(TAG, "HttpResponseException statusCode " + statusCode);
	      if (statusCode == 401 || statusCode == 403) {
	        gotAccount(true);
	      }
	      return;
	    }
	  }
}
