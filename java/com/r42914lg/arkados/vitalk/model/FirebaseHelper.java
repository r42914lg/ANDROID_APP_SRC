package com.r42914lg.arkados.vitalk.model;

import static com.r42914lg.arkados.vitalk.ViTalkConstants.LOG;

import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import org.jetbrains.annotations.NotNull;
import java.io.File;

import javax.inject.Inject;

public class FirebaseHelper {
    public static final String TAG = "LG> FirebaseHelper";

    private IDataLoaderListener dataLoaderListener;

    @Inject
    public FirebaseHelper(IDataLoaderListener dataLoaderListener) {
        this.dataLoaderListener = dataLoaderListener;
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull @NotNull Task<AuthResult> task) {
                dataLoaderListener.callbackFirebaseAuthenticated();
            }
        });

        if (LOG) {
            Log.d(TAG, " instance created");
        }
    }

    public void onClear() {
        dataLoaderListener = null;
    }

    public void uploadAudioWithId(String youTubeId,String googleAccId, String fullPath) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference islandRef = storageRef.child(googleAccId + youTubeId);

        Uri file = Uri.fromFile(new File(fullPath));
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("audio/mpeg")
                .build();

        UploadTask uploadTask = islandRef.putFile(file, metadata);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                if (dataLoaderListener ==  null) {
                    if (LOG) {
                        Log.d(TAG, ".uploadAudioWithId.onFailure listener  is NULL");
                    }
                    return;
                }
                dataLoaderListener.onFirebaseUploadFailed(fullPath);
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                if (dataLoaderListener ==  null) {
                    if (LOG) {
                        Log.d(TAG, ".uploadAudioWithId.onFailure listener  is NULL");
                    }
                    return;
                }
                dataLoaderListener.onFirebaseUploadFinished(youTubeId);
            }
        });

        if (LOG) {
            Log.d(TAG, ".uploadAudioWithId ID --> " +  youTubeId);
        }
    }
}
