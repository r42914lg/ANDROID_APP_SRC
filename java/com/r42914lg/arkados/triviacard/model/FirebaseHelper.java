package com.r42914lg.arkados.triviacard.model;

import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.billingclient.api.Purchase;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.r42914lg.arkados.triviacard.TriviaCardConstants;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import static com.r42914lg.arkados.triviacard.TriviaCardConstants.APP_PACKAGE_NAME;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.BUFFER_SIZE_LIMIT;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.FUNCTION_NAME;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.JSON_IMAGES;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.JSON_QUESTIONS;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.JSON_QUESTION_USAGE;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.JSON_QUIZZES;
import static com.r42914lg.arkados.triviacard.TriviaCardConstants.LOG;

public class FirebaseHelper {
    public static final String TAG = "LG> FirebaseHelper";

    private IDataLoaderListener dataLoaderListener;
    private LocalStorageHelper localStorageHelper;

    public FirebaseHelper(LocalStorageHelper localStorageHelper, IDataLoaderListener dataLoaderListener) {
        this.localStorageHelper = localStorageHelper;

        this.dataLoaderListener = dataLoaderListener;
        if (LOG) {
            Log.d(TAG, ".setDataLoaderListener: listener set " + dataLoaderListener);
        }

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signInAnonymously()
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull @NotNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    if (LOG) {
                        Log.d(TAG, "signInAnonymously:success");
                    }
                } else {
                    if (LOG) {
                        Log.d(TAG, "signInAnonymously:failure", task.getException());
                    }
                }
                dataLoaderListener.callbackFirebaseAuthenticated();
            }
        });

        if (LOG) {
            Log.d(TAG, ": instance created");
        }
    }

    public void onClear() {
        dataLoaderListener = null;
        localStorageHelper = null;

        if (LOG) {
            Log.d(TAG, ".onClear: instance created");
        }
    }

    public void subscribeForLoadImageFromFullPath(String fullPath, TriviaCardQuestion triviaCardQuestion) {
        if (LOG) {
            Log.d(TAG, ".subscribeForLoadImageFromFullPath: online Storage ord # in quiz = " + triviaCardQuestion.getOrdIndexInQuiz());
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference islandRef = storageRef.child(fullPath);

        islandRef.getBytes(BUFFER_SIZE_LIMIT).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                if (dataLoaderListener ==  null) {
                    return;
                }

                dataLoaderListener.callbackLoadImageFromFullPath(bytes, fullPath);

                if (LOG) {
                    Log.d(TAG, ".subscribeForLoadImageFromFullPath.onSuccess: doing callback for question ord # = in quiz " + triviaCardQuestion.getOrdIndexInQuiz());
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                if (dataLoaderListener ==  null) {
                    return;
                }

                dataLoaderListener.handleSchemaMismatch();

                if (LOG) {
                    Log.d(TAG, ".subscribeForLoadImageFromFullPath.onFailure: Failed to load image" + fullPath, exception);
                }
            }
        });
    }

    public void subscribeForLoadIconFromFullPath(String fullPath, JSON_Quiz jsonQuiz) {
        if (LOG) {
            Log.d(TAG, ".subscribeForLoadIconFromFullPath: fullPath = " + fullPath);
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference islandRef = storageRef.child(fullPath);

        islandRef.getBytes(BUFFER_SIZE_LIMIT).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                if (dataLoaderListener ==  null) {
                    return;
                }

                dataLoaderListener.callbackLoadIconFromFullPath(bytes, fullPath, jsonQuiz);

                if (LOG) {
                    Log.d(TAG, ".subscribeForLoadIconFromFullPath.onSuccess: doing callback for quiz ID =  " + jsonQuiz.getId());
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                if (dataLoaderListener ==  null) {
                    return;
                }

                dataLoaderListener.handleSchemaMismatch();

                if (LOG) {
                    Log.d(TAG, ".subscribeForLoadImageFromFullPath.onFailure: Failed to load image" + fullPath, exception);
                }
            }
        });
    }

    public void subscribeForImageById(String id, TriviaCardQuestion triviaCardQuestion) {
        if (LOG) {
            Log.d(TAG, ".subscribeForImageById: id = " + id);
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(JSON_IMAGES + "/" + id);

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataLoaderListener ==  null) {
                    return;
                }

                String key = dataSnapshot.getKey();
                JSON_Image jsonImage = dataSnapshot.getValue(JSON_Image.class);
                if (jsonImage != null) {
                    jsonImage.setId(key);
                    if (!jsonImage.assertIsValid()) {
                        dataLoaderListener.handleSchemaMismatch();
                        return;
                    }
                    if (LOG) {
                        Log.d(TAG, ".subscribeForImageById.onDataChange: jsonImage found with id = " + key + " - doing callback");
                    }
                    dataLoaderListener.callbackImageById(jsonImage, triviaCardQuestion);
                } else {
                    if (LOG) {
                        Log.d(TAG, ".subscribeForImageById.onDataChange: jsonImage NOT found with id = " + key + " - no callback");
                    }
                    dataLoaderListener.handleSchemaMismatch();
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                dataLoaderListener.handleSchemaMismatch();

                if (LOG) {
                    Log.d(TAG, ".subscribeForImageById.onFailure: Failed to load JSON image with id = " + id, error.toException());
                }
            }
        });
    }

    public void subscribeForQuizzesList(TriviaCardVM triviaCardVM) {
        if (LOG) {
            Log.d(TAG, ".subscribeForQuizzesList");
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(JSON_QUIZZES);

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataLoaderListener ==  null) {
                    return;
                }

                List<JSON_Quiz> activeQuizzes = new ArrayList<>();
                Iterable<DataSnapshot> quizzesList = dataSnapshot.getChildren();
                boolean jsonMismatchIdentified = !quizzesList.iterator().hasNext();

                for (DataSnapshot ds : quizzesList) {
                    String key = ds.getKey();
                    JSON_Quiz quiz = ds.getValue(JSON_Quiz.class);
                    if (quiz != null && quiz.getActive()) {
                        quiz.setId(key);
                        if (!quiz.assertIsValid()) {
                            jsonMismatchIdentified = true;
                            break;
                        }
                        quiz.setIsLocal(false);
                        quiz.setHighScore(localStorageHelper.readPointsByQiD(key));
                        quiz.setLastPlayedDate(localStorageHelper.readLastPlayedDateByQiD(key));
                        if (!triviaCardVM.checkImageLoaded(quiz.getIcon_full_path())) {
                            subscribeForLoadIconFromFullPath(quiz.getIcon_full_path(), quiz);
                        }
                        activeQuizzes.add(quiz);
                        if (LOG) {
                            Log.d(TAG, ".subscribeForQuizzesList.onDataChange: active quiz with id = " + key + "added to list");
                        }
                    }
                }

                if (jsonMismatchIdentified) {
                    if (LOG) {
                        Log.d(TAG, ".subscribeForQuizzesList.onDataChange: calling handle schema mismatch");
                    }
                    dataLoaderListener.handleSchemaMismatch();
                } else {
                    if (LOG) {
                        Log.d(TAG, ".subscribeForQuizzesList.onDataChange: total active quizzes count in list = " + activeQuizzes.size() + " - doing callback");
                    }
                    dataLoaderListener.callbackActiveQuizzesList(activeQuizzes);
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                if (dataLoaderListener ==  null) {
                    return;
                }
                if (LOG) {
                    Log.d(TAG, ".subscribeForQuizzesList.onCancelled: failed to get active quizzes", error.toException());
                }
                dataLoaderListener.handleSchemaMismatch();
            }
        });
    }

    public void subscribeForActiveQuestionsGivenQuiz(TriviaCardQuiz triviaCardQuiz) {
        if (LOG) {
            Log.d(TAG, ".subscribeForActiveQuestionsGivenQuiz: quiz id = " + triviaCardQuiz.getQuizId());
        }

        List<String> questionIDs = triviaCardQuiz.getQuestionIDs();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(JSON_QUESTIONS);

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataLoaderListener ==  null) {
                    return;
                }

                List<JSON_Question> activeQuestions = new ArrayList<>();
                Iterable<DataSnapshot> questionList = dataSnapshot.getChildren();
                boolean jsonMismatchIdentified = !questionList.iterator().hasNext();

                for (DataSnapshot ds : questionList) {
                    String key = ds.getKey();
                    if (questionIDs.contains(key)) {
                        JSON_Question question = ds.getValue(JSON_Question.class);
                        if (question != null && question.getActive()) {
                            question.setId(key);
                            if (!question.assertIsValid()) {
                                jsonMismatchIdentified = true;
                                break;
                            }
                            activeQuestions.add(question);
                        }
                    }
                }

                if (jsonMismatchIdentified || activeQuestions.size() < TriviaCardConstants.NUM_OF_QUESTIONS_IN_QUIZ - triviaCardQuiz.getCurrentQuestionIndex()) {
                    if (LOG) {
                        Log.d(TAG, ".subscribeForActiveQuestionsGivenQuiz.onDataChange: calling handle schema mismatch");
                    }
                    dataLoaderListener.handleSchemaMismatch();
                } else {
                    if (LOG) {
                        Log.d(TAG, ".subscribeForActiveQuestionsGivenQuiz.onDataChange: questions count = " + activeQuestions.size() + " -  doing callback");
                    }
                    dataLoaderListener.callbackActiveQuestionsGivenQuiz(activeQuestions);
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                if (dataLoaderListener ==  null) {
                    return;
                }
                dataLoaderListener.handleSchemaMismatch();
                if (LOG) {
                    Log.d(TAG, ".subscribeForActiveQuestionsGivenQuiz.onCancelled: failed to get questions for quiz id = " + triviaCardQuiz.getQuizId(), error.toException());
                }
            }
        });
    }

    public void checkPurchaseTokenLegitimate(Purchase purchase) {
        checkPurchaseTokenLegitimate(purchase.getSkus().get(0), purchase.getPurchaseToken(), purchase);
    }

    public void checkPurchaseTokenLegitimate(String skuId, String purchaseToken, Purchase purchase) {
        verifySubscription(skuId, purchaseToken).addOnCompleteListener(new OnCompleteListener<Map>() {
            @Override
            public void onComplete(@NonNull Task<Map> task) {
                if (dataLoaderListener ==  null) {
                    return;
                }

                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    if (LOG) {
                        Log.d(TAG, "checkPurchaseTokenLegitimate.onComplete: FAILURE", e);
                    }
                    dataLoaderListener.callbackPurchaseTokenLegitimate_FAILURE(purchase);
                } else {
                    Map result = task.getResult();
                    int status = (int) result.get("status");
                    if (status == 200) {
                        long expiryTimeMillis = Long.parseLong((String) result.get("expiryTimeMillis"));
                        boolean autoRenewing = (boolean) result.get("autoRenewing");

                        int paymentState = -1;
                        Object object = result.get("paymentState");
                        if (object == null) {
                            if (LOG) {
                                Log.d(TAG, "checkPurchaseTokenLegitimate.onComplete: paymentState is N/A");
                            }
                        } else {
                            paymentState = (int) object;
                        }
                        if (LOG) {
                            Log.d(TAG, "checkPurchaseTokenLegitimate.onComplete: 200");
                        }
                        dataLoaderListener.callbackPurchaseTokenLegitimate_200(purchase, expiryTimeMillis, paymentState, autoRenewing);
                    } else if (status == 500)  {
                        if (LOG) {
                            Log.d(TAG, "checkPurchaseTokenLegitimate.onComplete: 500");
                        }
                        dataLoaderListener.callbackPurchaseTokenLegitimate_500(purchase);
                    }
                }
            }
        });
    }

    private Task<Map> verifySubscription(String skuId, String purchaseToken) {
        FirebaseFunctions functions = FirebaseFunctions.getInstance();
        ArrayMap<String, String> data = new ArrayMap<String, String>();
        data.put("sku_id",skuId);
        data.put("purchase_token",purchaseToken);
        data.put("package_name",APP_PACKAGE_NAME);

        return functions.getHttpsCallable(FUNCTION_NAME).call(data).continueWith(new Continuation<HttpsCallableResult, Map>() {
            @NonNull
            @Override
            public Map then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                return (Map) task.getResult().getData();
            }
        });
    }

    public void incrementFreqByQiD(List<String> questionIDs) {
        if (LOG) {
            Log.d(TAG, ".incrementFreqByQiD: question IDs = " + questionIDs);
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(JSON_QUESTION_USAGE);
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterable<DataSnapshot> list = dataSnapshot.getChildren();
                for (DataSnapshot ds : list) {
                    if (questionIDs.contains(ds.getKey())) {
                        Long value = (Long) ds.getValue();
                        myRef.child(ds.getKey()).setValue(value + 1);
                        if (LOG) {
                            Log.d(TAG, ".incrementFreqByQiD.onDataChange: QiD = " + ds.getKey() + " - new value set --> " + (value + 1));
                        }
                        questionIDs.remove(ds.getKey());
                    }
                }
                for (String qId : questionIDs) {
                    myRef.child(qId).setValue(1);
                    if (LOG) {
                        Log.d(TAG, ".incrementFreqByQiD.onDataChange: QiD = " + qId + " - init value set --> 1");
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                if (LOG) {
                    Log.d(TAG, ".incrementFreqByQiD.onCancelled: failed", error.toException());
                }
            }
        });
    }
}
