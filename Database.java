import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.json.JSONObject;

public class Database {

    private String url;
    private URI uri;
    private SSLSocket socket;
    private HttpURLConnection connection;
    private DatabaseListener iListener;

    private String prefix_path;
    private String host;

    private Thread thread;
    private STATE state = STATE.DISCONNECTED;
    private SocketReader readr;
    private SocketWriter wrter;
    
    private final static int PING_TIME = 10 * 1000;

public Database(String link){
    url = link.endsWith(".json") ? link.substring(0,link.length()-(5)):link;
    uri = URI.create(url);
    host = uri.getHost();
    prefix_path = uri.getPath();
}
public synchronized boolean isConnecting(){
    return state == STATE.CONNECTING;
}
public synchronized void connect(){
    if(state == STATE.DISCONNECTED){
        state = STATE.CONNECTING;
thread = new Thread(){
public void run(){
try{
    readr = new SocketReader();
    wrter = new SocketWriter();
readr.connect();
wrter.connect();
state = STATE.CONNECTED;
}catch(Exception e){
 state = STATE.DISCONNECTED;
 e.printStackTrace();
}}
};
thread.start();
    }
}
public synchronized void disconnect(){
    if(state != STATE.DISCONNECTED){
state = STATE.DISCONNECTED;
readr.close();
wrter.close();
}
}
public synchronized boolean isConnected(){
    if(wrter == null) return false;
    if(readr == null) return false;
return wrter.isConnected() && readr.isConnected();
}
public void get(String pth){
   Task task = new Task();
   task.setPath(pth);
   task.setRequest("GET");
   task.type = TASK_TYPE.GET;
   task.build();
   wrter.tasks.add(task);
}
public void get(){
    Task task = new Task();
   task.setRequest("GET");
   task.type = TASK_TYPE.GET;
   task.build();
   wrter.tasks.add(task);
   
}
public void put(String path,String data){
   Task task = new Task();
   task.setPath(path);
   task.setRequest("PUT");
   task.setPayload(data.getBytes());
   task.type = TASK_TYPE.PUT;
   task.build();
   wrter.tasks.add(task);
}
public void put(String data){
   Task task = new Task();
   task.setRequest("PUT");
   task.setPayload(data.getBytes());
   task.type = TASK_TYPE.PUT;
    task.build();
    wrter.tasks.add(task);
}

    public void post(String path,String data){
        Task task = new Task();
        task.setPath(path);
        task.setRequest("POST");
        task.setPayload(data.getBytes());
        task.type = TASK_TYPE.POST;
        task.build();
        wrter.tasks.add(task);
    }
    public void post(String data){
        Task task = new Task();
        task.setRequest("POST");
        task.setPayload(data.getBytes());
        task.type = TASK_TYPE.POST;
        task.build();
        wrter.tasks.add(task);
    }
    public synchronized int remainingRequest(){
        if(wrter == null) return -1;
        return wrter.tasks.size();
    }

public void patch(String path,String data){
    Task task = new Task();
    task.setPath(path);
    task.setRequest("PATCH");
    task.setPayload(data.getBytes());
    task.type = TASK_TYPE.PATCH;
    task.build();
    wrter.tasks.add(task);
}
public void patch(String data){
    Task task = new Task();
   task.setRequest("PATCH");
   task.setPayload(data.getBytes());
   task.type = TASK_TYPE.PATCH;
    task.build();
    wrter.tasks.add(task);
}
public void delete(String path){
    Task task = new Task();
    task.setPath(path);
    task.setRequest("DELETE");
    task.type = TASK_TYPE.DELETE;
    task.build();
    wrter.tasks.add(task);
}
public void delete(){
    Task task = new Task();
    task.setRequest("DELETE");
    task.type = TASK_TYPE.DELETE;
    task.build();
    wrter.tasks.add(task);
}
public void Sum(String path,int delta){
    Task task = new Task();
    task.setPath(path);
    task.setRequest("PUT");
    task.setPayload(("{\".sv\": {\"increment\":"+delta+"}}").getBytes());
    task.type = TASK_TYPE.PUT;
    task.build();
    wrter.tasks.add(task);
}
public void Sum(String path,float delta){
    Task task = new Task();
    task.setPath(path);
    task.setRequest("PUT");
    task.setPayload(("{\".sv\": {\"increment\":"+delta+"}}").getBytes());
    task.type = TASK_TYPE.PUT;
    task.build();
    wrter.tasks.add(task);
}
public void Sum(int delta){
    Task task = new Task();
    task.setRequest("PUT");
    task.setPayload(("{\".sv\": {\"increment\":"+delta+"}}").getBytes());
    task.type = TASK_TYPE.PUT;
    task.build();
    wrter.tasks.add(task);
}
public void Sum(float delta){
    Task task = new Task();
    task.setRequest("PUT");
    task.setPayload(("{\".sv\": {\"increment\":"+delta+"}}").getBytes());
    task.type = TASK_TYPE.PUT;
    task.build();
    wrter.tasks.add(task);
}

public void addEventListener(DatabaseListener listener){
iListener = listener;
}
public void removeEventListener(){
    iListener = null;
    }


    class SocketWriter extends Thread{
        DataInputStream dis;
        OutputStream outputStream;
        Task tmp;
        byte[] getPing(){
        return ("GET /ws HTTP/1.1\r\n"+
        "Host: "+host+"\r\n"+
        "Upgrade: websocket\r\n"+
        "Connection: Upgrade\r\n"+
        "Connection: keep-alive\r\n"+
        "Sec-WebSocket-Version: 13\r\n\r\n").getBytes();
        }

        long lastPing = 0;

        ArrayDeque<Task> tasks = new ArrayDeque<Task>();
        void connect() throws Exception{
            SocketFactory factory = SSLSocketFactory.getDefault();
            socket = (SSLSocket)factory.createSocket(host, 443);
            SSLSession session = socket.getSession();
            HostnameVerifier verifier = HttpsURLConnection.getDefaultHostnameVerifier();
            if(!verifier.verify(host,session)){
                disconnect();
            }
            dis = new DataInputStream(socket.getInputStream());
            outputStream = socket.getOutputStream();
            start();
        }
        boolean isConnected(){
            if(socket == null) return false;
        return socket.isConnected();
        }
        void close(){
            if(socket != null){
                try{
                    socket.close();
                }catch(Exception e){

                }
            }
        }
        @Override
        public void run() {
            try{
           while (true) {
            if(lastPing < Instant.now().toEpochMilli()){
            lastPing = Instant.now().toEpochMilli() + PING_TIME;
            outputStream.write(getPing());
            readResponse();
            }
            if(tasks.size() > 0){
            lastPing = Instant.now().toEpochMilli() + PING_TIME;
              tmp =  tasks.pop();
              outputStream.write(tmp.request);
              if(tmp.payload != null){
              outputStream.write(tmp.payload);
              }
              if(!readResponse()){
               onFailed();
              }
            }
           }
        }catch(Exception e){
            disconnect(); 
            e.printStackTrace();
        }
    }

    boolean readResponse() throws Exception{
        boolean handshakeComplete = false;
      int len = 1000;
      byte[] buffer = new byte[len];
      int pos = 0;
      ArrayList<String> handshakeLines = new ArrayList<String>();

      while (!handshakeComplete) {
        int b = dis.read();
        buffer[pos] = (byte) b;
        pos += 1;

        if (buffer[pos - 1] == 0x0A && buffer[pos - 2] == 0x0D) {
          String line = new String(buffer);
          if (line.trim().equals("")) {
            handshakeComplete = true;
          } else {
            handshakeLines.add(line.trim());
          }
          buffer = new byte[len];
          pos = 0;
        } else if (pos == 1000) {
        
        }
      }
      for(String line : handshakeLines){
        if(line.startsWith("Content-Length")){
            int size = Integer.parseInt(line.split(": ")[1]);
            if(size == 0) break;
            byte[] aee = new byte[size];
            dis.read(aee,0,size);
            onResponse( new String(aee));
        }
      }
      
        for(String line : handshakeLines){
            if(line.startsWith("HTTP/1.1")){
             String code = line.split(" ")[1];
             if(code.equals("301")||code.equals("200")){
              handshakeLines.clear();
              return true; 
             }
            }
        }
       handshakeLines.clear();
       return false;
    }
    
    void onResponse(String data){
        if(iListener != null){
            
            switch(tmp.type){
                case GET:
                   iListener.onGetComplete(tmp.path,data);
                    break;
                case PATCH:
                    iListener.onPatchComplete(tmp.path,data);
                    break;
                 case PUT:
                    iListener.onPutComplete(tmp.path,data);
                     break;
                 case POST:
                     iListener.onPostComplete(tmp.path,data);
                     break;
                 case DELETE:
                     iListener.onDeleteComplete(tmp.path);
                     break;
            }
            
           }
    }
    
    void onFailed(){
        if(iListener != null){
            String data = null;
            if(tmp.payload != null){
             data = new String(tmp.payload);
            }
            switch(tmp.type){
                case GET:
                    iListener.onGetFailed(tmp.path,data);
                    break;
                case PATCH:
                    iListener.onPatchFailed(tmp.path,data);
                    break;
                case PUT:
                    iListener.onPutFailed(tmp.path,data);
                    break;
                case POST:
                    iListener.onPostFailed(tmp.path,data);
                    break;
                case DELETE:
                    iListener.onDeleteFailed(tmp.path);
                    break;
            }

        }
    }
   

    }
    class SocketReader extends Thread{
        BufferedReader reader;
        void connect() throws Exception{
            connection = (HttpURLConnection) new URL(url+".json").openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept","text/event-stream");
                reader = new BufferedReader(new InputStreamReader( connection.getInputStream()));
            start();
        }
        boolean isConnected(){
        return reader != null;
        }
        void close(){
            try{
                if(connection == null){
                 return;
                }
            connection.disconnect();
            reader = null;
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try{
                String line =null;
                while((line = reader.readLine()) != null){
                line = line.trim();
                if(line.startsWith("event")){
                    if(iListener == null) continue;
                    String event = line.split(": ")[1];
                    String data = reader.readLine().trim();
                    
                    if(event.equals("keep-alive")){
                    continue;
                    }
                    JSONObject js = new JSONObject(data.split(": ")[1]);
                    if(event.equals("patch")){
                        iListener.onPut(js.getString("path"),js.get("data").toString());
                    }
                    if(event.equals("put")){
                        String dat = js.get("data").toString();
                        if(dat.equals("null")){
                            iListener.onDelete(js.getString("path"));
                            }else{
                        iListener.onPut(js.getString("path"),dat);
                        }
                    }
                }
                }
            }catch(Exception e){
            disconnect();
            }
            disconnect();
        }
    }
    enum STATE{
        CONNECTING,CONNECTED,DISCONNECTED
    }
    enum TASK_TYPE{
        PUT,PATCH,DELETE,GET,POST
    }

    class Task{
        byte[] request;
        byte[] payload;
        String path;
        String method;
        TASK_TYPE type = TASK_TYPE.GET;
        Task(){
        
        }
        void setRequest(String mth){
        method = mth;
        }
        void setPath(String pth){
        path = pth;
        }
        void setPayload(byte[] data){
        payload = data;
        }
        void build(){
            String ps = prefix_path;
            ps += "/";
            if(path != null){
                ps += path.startsWith("/")?path.substring(1):path;
            }
        String tmp = method+" "+ps+".json HTTP/1.1\r\n"+
    "Host: "+host+"\r\n"+
    "Upgrade: websocket\r\n"+
    "Connection: Upgrade\r\n"+
    "Connection: keep-alive\r\n"+
    (payload != null?"Content-Length: "+payload.length+"\r\n":"" )+
    "Sec-WebSocket-Version: 13\r\n\r\n";
    request = tmp.getBytes();
    }

    }
}
