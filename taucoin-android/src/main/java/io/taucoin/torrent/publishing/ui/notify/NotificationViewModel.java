package io.taucoin.torrent.publishing.ui.notify;

import android.app.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.core.model.data.NotificationAndUser;
import io.taucoin.torrent.publishing.core.model.data.Result;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Notification;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.NotificationRepository;
import io.taucoin.torrent.publishing.core.utils.ChainLinkUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;

/**
 * Community模块的ViewModel
 */
public class NotificationViewModel extends AndroidViewModel {

    private static final Logger logger = LoggerFactory.getLogger("NotificationViewModel");
    private NotificationRepository notifyRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MutableLiveData<Result> deleteState = new MutableLiveData<>();

    public NotificationViewModel(@NonNull Application application) {
        super(application);
        notifyRepo = RepositoryHelper.getNotificationRepository(getApplication());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }

    public MutableLiveData<Result> getDeleteState() {
        return deleteState;
    }

    /**
     * 和联系人创建Chat
     * @param selectedList List<NotificationAndUser>
     */
    void deleteNotifications(List<NotificationAndUser> selectedList) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Result>) emitter -> {
            Result result = new Result();
            try {
                NotificationAndUser[] deleteList = selectedList.toArray(new NotificationAndUser[]{});
                notifyRepo.deleteNotifications(deleteList);
            }catch (Exception e){
                result.setFailMsg(e.getMessage());
            }
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> deleteState.postValue(state));
        disposables.add(disposable);
    }

    /**
     * 查询所有通知
     * @return DataSource.Factory
     */
    DataSource.Factory<Integer, NotificationAndUser> queryNotifications() {
        return notifyRepo.queryNotifications();
    }

    void readAllNotifications() {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Result>) emitter -> {
            notifyRepo.readAllNotifications();
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        disposables.add(disposable);
    }

    /**
     * 添加通知
     */
    public void addNotification(String inviteLink, String friendPk){
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Result>) emitter -> {
            ChainLinkUtil.ChainLink decode = ChainLinkUtil.decode(inviteLink);
            if(decode.isValid()){
                String chainID = decode.getDn();
                Notification notification = notifyRepo.queryNotification(friendPk, chainID);
                if(null == notification){
                    notification = new Notification(friendPk, inviteLink,
                            chainID, DateUtil.getTime());
                    notifyRepo.addNotification(notification);
                }
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
        disposables.add(disposable);
    }

    /**
     * 查询未读通知的数目
     * @return Flowable
     */
    public Flowable<Integer> queryUnreadNotificationsNum() {
        return notifyRepo.queryUnreadNotificationsNum();
    }
}
