package com.sahilgupta.newsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
        ArrayList<String> arrayList=new ArrayList<String>();
        ArrayList<String> contentlist=new ArrayList<String>();
        ArrayAdapter arrayAdapter;
        SQLiteDatabase articlesDb;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView listView=(ListView)findViewById(R.id.listView);
        arrayAdapter=new ArrayAdapter(this,android.R.layout.simple_list_item_1,arrayList);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent=new Intent(getApplicationContext(),ArticleActivity.class);
                intent.putExtra("content", contentlist.get(position));
                startActivity(intent);
            }
        });
        articlesDb=this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        articlesDb.execSQL("create table if not exists articles (id integer primary key,articleId integer, title varchar, content varchar)");
        update();
        downloadtask d=new downloadtask();
        try {
            d.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    public void update()
    {
        Cursor c= articlesDb.rawQuery("Select * from articles",null);
        int contentIndex=c.getColumnIndex("content");
        int titleIndex=c.getColumnIndex("title");
        if(c.moveToFirst())
        {
            arrayList.clear();
            contentlist.clear();
            do{
                arrayList.add(c.getString(titleIndex));
                contentlist.add(c.getString(contentIndex));


            }while(c.moveToNext());
            arrayAdapter.notifyDataSetChanged();
        }
    }
    public class downloadtask extends AsyncTask<String,Void,String>
    {

        @Override
        protected String doInBackground(String... strings) {
            String result="";
            try {
                URL url;
                HttpURLConnection urlConnection = null;
                url = new URL(strings[0]);
                urlConnection=(HttpURLConnection)url.openConnection();
                InputStream in=urlConnection.getInputStream();
                InputStreamReader reader=new InputStreamReader(in);
                int data=reader.read();
                while(data!=-1)
                {
                    char c=(char)data;
                    result=result+c;
                    data=reader.read();
                }
                JSONArray jsonArray=new JSONArray(result);
                int number=20;
                if(jsonArray.length()<20)
                {
                    number=jsonArray.length();
                }
                articlesDb.execSQL("Delete from articles");
                for(int i=0;i<number;i++)
                {
                    String articleid=jsonArray.getString(i);
                    url=new URL("https://hacker-news.firebaseio.com/v0/item/"+articleid+".json?print=pretty");
                    urlConnection=(HttpURLConnection)url.openConnection();
                    in=urlConnection.getInputStream();
                    reader=new InputStreamReader(in);
                    int d=reader.read();
                    String info="";
                    while (d!=-1)
                    {
                        char current=(char)d;
                        info+=current;
                        d=reader.read();
                    }
                    JSONObject jsonObject=new JSONObject(info);
                    if(!jsonObject.isNull("title")&&!jsonObject.isNull("url") ) {
                        String title = jsonObject.getString("title");
                        String articleurl = jsonObject.getString("url");
                        url=new URL(articleurl);
                        urlConnection=(HttpURLConnection)url.openConnection();
                        in=urlConnection.getInputStream();
                        reader=new InputStreamReader(in);
                        int dop=reader.read();
                        String content="";
                        while (dop!=-1)
                        {
                            char curr=(char)dop;
                            content+=curr;
                            dop=reader.read();
                        }
                        String sql="insert into articles (articleId, title, content) values (? , ? , ?)";
                        SQLiteStatement statement=articlesDb.compileStatement(sql);
                        statement.bindString(1,articleid);
                        statement.bindString(2,title);
                        statement.bindString(3,content);
                        statement.execute();
                    }


                }

            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            return null;
        }
        protected void onPostExecute(String s){
            super.onPostExecute(s);
            update();

        }
    }
}
