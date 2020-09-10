package io.taucoin.torrent.publishing.ui.chat;

import android.app.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.core.model.data.MsgAndReply;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MsgRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Message;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.ui.constant.Page;

/**
 * 消息模块相关的ViewModel
 */
public class MsgViewModel extends AndroidViewModel {

    private static final Logger logger = LoggerFactory.getLogger("MsgViewModel");
    private MsgRepository msgRepo;
    private UserRepository userRepo;
    private MsgSourceFactory sourceFactory;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MutableLiveData<List<Message>> msgList = new MutableLiveData<>();
    private MutableLiveData<String> addState = new MutableLiveData<>();

    public MsgViewModel(@NonNull Application application) {
        super(application);
        msgRepo = RepositoryHelper.getMsgRepository(application);
        userRepo = RepositoryHelper.getUserRepository(application);
        sourceFactory = new MsgSourceFactory(msgRepo);
    }

    /**
     * 观察社区消息
     * @param chainID 社区chainID
     * @return LiveData
     */
    LiveData<PagedList<MsgAndReply>> observeMessages(String chainID) {
        sourceFactory.setChainID(chainID);
        return new LivePagedListBuilder<>(sourceFactory, Page.getPageListConfig())
                .setInitialLoadKey(Page.PAGE_SIZE)
                .build();
    }

    MutableLiveData<String> getAddState() {
        return addState;
    }

    public void setAddState(MutableLiveData<String> addState) {
        this.addState = addState;
    }

    /**
     * 获取社区链消息列表的被观察者
     * @return 被观察者
     */
    public MutableLiveData<List<Message>> getMsgListState() {
        return msgList;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }

    /**
     * 根据chainID查询社区的消息
     * @param chainID 社区链id
     */
    public void getMessagesByChainID(String chainID){
        Disposable disposable = Flowable.create((FlowableOnSubscribe<List<Message>>) emitter -> {
            List<Message> msgList = msgRepo.getMessagesByChainID(chainID);
            emitter.onNext(msgList);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> msgList.postValue(list));
        disposables.add(disposable);
    }

    /**
     * 发送消息
     * @param msg 消息
     */
    void sendMessage(Message msg) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<String>) emitter -> {
            String result = "";
            try {
                // 获取当前用户的公钥
                User currentUser = userRepo.getCurrentUser();
                msg.senderPk = currentUser.publicKey;
                msg.timestamp = DateUtil.getTime();
                msg.msgID = DateUtil.getCurrentTime();
                // TODO: 发送消息到DHT
                msgRepo.sendMessage(msg);
            }catch (Exception e){
                result = e.getMessage();
                logger.debug("Error sending message::{}", result);
            }
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> addState.postValue(state));
        disposables.add(disposable);
    }
}