public interface DatabaseListener {

    void onPatch(String path,String data);
    void onPut(String path,String data);
    void onDelete(String path);

    void onPatchComplete(String path,String data);
    void onPutComplete(String path,String data);
    void onPostComplete(String path,String data);
    void onDeleteComplete(String path);
    void onGetComplete(String path,String data);

    void onPatchFailed(String path,String data);
    void onPutFailed(String path,String data);
    void onPostFailed(String path,String data);
    void onDeleteFailed(String path);
    void onGetFailed(String path,String data);
}
