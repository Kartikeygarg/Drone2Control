package com.example.defenselabs.dronecontrol;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;


public class SecondActivity extends AppCompatActivity implements View.OnTouchListener {

    ImageView frontIV , topIV;
    Bitmap bitmap_front, bitmap_top;
    double init_lat, init_long, init_height;
    double curr_lat, curr_long, curr_height;
    Button b1, btn_view;
    Socket socket = null;
    int max_horizontal_angle = 70, max_vertical_angle = 45, max_depth = 200;
    boolean first_location = true;
    int frontIV_width = 1199, frontIV_height = 720;
    int topIV_width = 1199, topIV_height = 720;
    Bitmap newBitmap;
    float aa = 400, bb = 400 , cc=400;
    GestureDetector gdt;
    PrintStream PS;
    byte send_array[], input_array[];
    OutputStream out;
    String dstAddress;
    int dstPort;
    boolean top_view =false, front_view = true;
    EditText editTextAddress, editTextPort;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        frontIV = (ImageView) findViewById(R.id.frontIV);
        topIV = (ImageView) findViewById(R.id.topIV);
        b1 = (Button) findViewById(R.id.btn1);
        btn_view = (Button) findViewById(R.id.btn_view);
        gdt = new GestureDetector(new GestureListenerFront());
        editTextAddress = (EditText)findViewById(R.id.address);
        editTextAddress.setText("192.168.1.6");
        editTextPort = (EditText)findViewById(R.id.port);
        editTextPort.setText("4444");
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                SendReceiveAsyncTask obj = new SendReceiveAsyncTask(editTextAddress.getText().toString(),Integer.parseInt(editTextPort.getText().toString()));
                obj.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                frontIV.setOnTouchListener(SecondActivity.this);
                b1.setVisibility(View.GONE);
            }
        });
        btn_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(front_view) {
                    btn_view.setText("TOP");
                    front_view = false;
                    top_view = true;
                    frontIV.setVisibility(View.GONE);
                    topIV.setVisibility(View.VISIBLE);

                }
                else
                {
                    btn_view.setText("FRONT");
                    front_view = true;
                    top_view = false;
                    frontIV.setVisibility(View.VISIBLE);
                    topIV.setVisibility(View.GONE);

                }
            }
        });


    }

    /*@Override
    public void onDestroy() {
        super.onDestroy();

    }*/

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {

        frontIV_width = frontIV.getWidth();
        frontIV_height = frontIV.getHeight();

        topIV_width = topIV.getWidth();
        topIV_height = topIV.getHeight();


        bitmap_front = Bitmap.createBitmap((int) getWindowManager()
                .getDefaultDisplay().getWidth(), (int) getWindowManager()
                .getDefaultDisplay().getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas_front = new Canvas(bitmap_front);

        Paint paint = new Paint();
        paint.setColor(Color.rgb(140, 0, 0));
        paint.setAlpha(80);
        paint.setStrokeWidth(2);
        Log.i("TAG", "width : " + frontIV_width + " Height : " + frontIV_height);
        canvas_front.drawLine(0, (float) (frontIV_height * 0.9), frontIV_width , (float) (frontIV_height * 0.9), paint);
        canvas_front.drawLine(frontIV_width / 2, 0, frontIV_width / 2, frontIV_height, paint);
        paint.setTextSize(26);
        canvas_front.drawText("West", 20, (float) (frontIV_height * 0.9)-5 , paint);
        canvas_front.drawText("East", frontIV_width - 75, (float) (frontIV_height * 0.9)-5, paint);
        canvas_front.save();
        canvas_front.rotate((float)  270 , frontIV_width/2 -10, 100);
        canvas_front.drawText("Height",frontIV_width/2 -10 ,100, paint);
        canvas_front.restore();
        frontIV.setImageBitmap(bitmap_front);

        bitmap_top = Bitmap.createBitmap((int) getWindowManager()
                .getDefaultDisplay().getWidth(), (int) getWindowManager()
                .getDefaultDisplay().getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas_top = new Canvas(bitmap_top);

        Paint paint_top = new Paint();
        paint_top.setColor(Color.rgb(0, 140, 0));
        paint_top.setAlpha(80);
        paint_top.setStrokeWidth(2);
        Log.i("TAG", "width : " + topIV_width + " Height : " +topIV_height);
        canvas_top.drawLine(0, (float) (topIV_height * 0.8), topIV_width, (float) (topIV_height * 0.8), paint_top);
        canvas_top.drawLine(topIV_width / 2, 0, topIV_width / 2, topIV_height, paint_top);

        paint_top.setTextSize(26);
        canvas_top.drawText("West", 20, (float) (topIV_height * 0.8)-5 , paint_top);
        canvas_top.drawText("East", topIV_width - 75, (float) (topIV_height * 0.8)-5, paint_top);
        canvas_top.save();
        canvas_top.rotate((float)  270 ,topIV_width/2 -10, 100);
        canvas_top.drawText("North",topIV_width/2 -10 ,100, paint);
        canvas_top.restore();
        topIV.setImageBitmap(bitmap_top);
        topIV.setVisibility(View.GONE);
    }

    public double getlongitudeoffset(double longitude) {
        return longitude - init_long;
    }

    public double getlatitudeoffset(double latitude) {
        return latitude - init_lat;
    }

    public float get_horizontal_disp(double latitude, double longitude) {
        double latitudeCircumference = 40075160 * Math.cos(init_lat * Math.PI / 180);
        double long_disp = (longitude - init_long) * latitudeCircumference / 360;
        double curr_depth = (latitude - init_lat) * 40008000 / 360;
        double max_disp = curr_depth * Math.tan(Math.toRadians(max_horizontal_angle));
        double disp = (long_disp / max_disp) * (frontIV.getWidth() / 2);

        return (float) ((frontIV_width / 2) + disp);
    }

    public float get_vertical_disp(double latitude, double altitude) {

        double vert_disp = (altitude - init_height);
        double curr_depth = (latitude - init_lat) * 40008000 / 360;
        double max_disp = curr_depth * Math.tan(Math.toRadians(max_vertical_angle));
        double disp = (vert_disp / max_disp) * (frontIV.getHeight() * 0.9);

        return (float) ((frontIV_height * 0.9) - disp);

    }

    public float get_depth_disp(double latitude)
    {
        double curr_depth = (latitude - init_lat) * 40008000 / 360;
        double disp = (curr_depth / max_depth) * (topIV_height * 0.9);
        return (float) ((frontIV_height * 0.8) - disp);
    }

    public double getLatitudeOffset(double latitude, double offset) {
        double R = 6378137;
        double latitudeCircumference = 40075160 * Math.cos(init_lat * Math.PI / 180);
        double new_latitude = latitude + ((offset / R) * (180 / Math.PI));
        return new_latitude;
    }

    private double getLongitudeOffset(double longitude, double offset) {
        double R = 6378137;
        double latitudeCircumference = 40075160 * Math.cos(init_lat * Math.PI / 180);
        double new_long  = init_long + (offset *360 / latitudeCircumference);
        //double new_longitude = longitude + (offset / R) * (180 / Math.PI) / Math.cos(longitude * Math.PI / 180);
        return new_long;

    }

    /* private double getlongitudeoffset(double longitude, double offset) {
         double R = 6378137;
         double new_longitude = longitude + (offset/R) * ( 180 /Math.PI) / Math.cos(longitude * Math.PI/180);
         return new_longitude;

     }*/
    public void drawDroneFront(float horiz_disp, float vert_disp) {
        aa = horiz_disp;
        bb = vert_disp;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                newBitmap = Bitmap.createBitmap(bitmap_front);
                Canvas canvas = new Canvas(newBitmap);
                Paint paint = new Paint();
                paint.setColor(Color.rgb(255, 0, 0));
                canvas.drawCircle(aa, bb, 20, paint);
                frontIV.setImageBitmap(newBitmap);
            }
        });

    }

    public void drawDroneTop(float horiz_disp, float depth_disp) {
        aa = horiz_disp;
        cc = depth_disp;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                newBitmap = Bitmap.createBitmap(bitmap_top);
                Canvas canvas = new Canvas(newBitmap);
                Paint paint = new Paint();
                paint.setColor(Color.rgb(0, 255, 0));
                canvas.drawCircle(aa, cc, 20, paint);
                topIV.setImageBitmap(newBitmap);
            }
        });

    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        gdt.onTouchEvent(motionEvent);
        return true;
    }


    public class SendReceiveAsyncTask extends AsyncTask<Void, Void, Void> {

        String dstAddress, cmd;
        int dstPort;

        SendReceiveAsyncTask(String addr, int port) {
            dstAddress = addr;
            dstPort = port;
            //this.cmd = cmd;
        }


        @Override
        protected Void doInBackground(Void... voids) {

            try {
                socket = new Socket(dstAddress, dstPort);
                InputStream in = socket.getInputStream();
                out = socket.getOutputStream();
                PS = new PrintStream(socket.getOutputStream());
                BufferedReader is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PS.println("SEND");
                while (true) {
                    // String message = is.readLine();
                    send_array = new byte[1024];
                    input_array = new byte[1024];


                    IOUtils.read(in, input_array);
                    String message = new String(input_array, "UTF-8");

                    if (first_location) {
                        first_location = false;
                        drawDroneFront(frontIV_width / 2, (float) (frontIV_height * 0.9));
                        drawDroneTop(topIV_width / 2, (float) (topIV_height * 0.8));
                        String a[] = message.split(":");
                        init_lat = Double.parseDouble(a[1]);
                        init_long = Double.parseDouble(a[3]);
                        init_height = Double.parseDouble(a[5]);

                        new DownloadImageTask().execute("dd");
                        // drawDrone(get_horizontal_disp(init_lat,init_long),get_vertical_disp(init_lat,init_height));

                        // PS.println("SEND");

                    } else {
                        String a[] = message.split(":");
                        curr_lat = Double.parseDouble(a[1]);
                        curr_long = Double.parseDouble(a[3]);
                        curr_height = Double.parseDouble(a[5]);
                        if(curr_lat==init_lat && curr_long==init_long && curr_height==init_height) {
                            //drawDroneFront(frontIV_width / 2, (float) (frontIV_height * 0.9));
                        }
                        else {
                            drawDroneFront(get_horizontal_disp(curr_lat, curr_long), get_vertical_disp(curr_lat, curr_height));
                            drawDroneTop(get_horizontal_disp(curr_lat, curr_long), get_depth_disp(curr_lat));

                        }

                        Log.i("TAG RECEIVING","Latitude:" + curr_lat + ":Longitude:" + curr_long + ":Height:" + curr_height + ":\n");
                            try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        // PS.println("SEND");
                    }
                    System.arraycopy(toByteArray("SEND"),0,send_array,0,4);
                    out.write(send_array,0,send_array.length);

                }


            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

    }

    byte[] toByteArray(String value) {
        return new byte[] {
                (byte)(value.charAt(0) ),
                (byte)(value.charAt(1) ),
                (byte)(value.charAt(2)),
                (byte)value.charAt(3)   };
    }

    private class GestureListenerFront extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
            float xPer = (event.getX() - (frontIV_width / 2)) / (frontIV_width / 2);
            float yPer = (float) (((frontIV_height * 0.9) - event.getY()) / (frontIV_height * 0.9));

            double curr_depth = (curr_lat - init_lat) * 40008000 / 360;
            double max_hori_disp = curr_depth * Math.tan(Math.toRadians(max_horizontal_angle));
            double max_vert_disp = curr_depth * Math.tan(Math.toRadians(max_vertical_angle));

            double new_hori_disp = max_hori_disp * xPer;
            double new_vert_disp = max_vert_disp * yPer;
            /*double latitudeCircumference = 40075160 * Math.cos(init_lat * Math.PI /180);
            double new_longitude = init_long + ((new_hori_disp *360)/ latitudeCircumference );*/

            final double new_longitude = getLongitudeOffset(init_long, new_hori_disp);
            //  double new_latitude = getLatitudeOffset( init_lat , new_hori_disp);
            final double new_height = init_height  + (yPer * max_vert_disp);
          //  final double new_height =  (yPer * max_vert_disp);
            SendAsyncTask obj = new SendAsyncTask(curr_lat, new_longitude, new_height);
            obj.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            return true;
        }
    }

    public class SendAsyncTask extends AsyncTask<Void, Void, Void> {

        double new_lat, new_long,new_height;

        SendAsyncTask(double new_lat,double new_long,double new_height) {
            this.new_height= new_height;
            this.new_lat= new_lat;
            this.new_long= new_long;
        }


        @Override
        protected Void doInBackground(Void... voids) {
            //   PS.println("Latitude:" + new_lat + ":Longitude:" + new_long + ":Height:" + new_height + ":\n");
            String str = "Latitude:" + new_lat + ":Longitude:" + new_long + ":Height:" + new_height + ":\n";
            byte dtn_array[] = new byte[1024];
            System.arraycopy(str.getBytes(),0,dtn_array,0,str.length());
            try {
                out.write(dtn_array,0,dtn_array.length);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.i("TAG SENDING","Latitude:" + new_lat + ":Longitude:" + new_long + ":Height:" + new_height + ":\n");
            return null;
        }
    }


    private class DownloadImageTask extends AsyncTask<String, Void, Void> {
        ImageView bmImage;


        @Override
        protected Void doInBackground(String... urls) {

            String urldisplay = urls[0];
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                bitmap_top = BitmapFactory.decodeStream(in);
                //


            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }


            return null;
        }
    }
}
