package com.xxl.rpc.admin.service.impl;

import com.xxl.rpc.admin.core.model.XxlRpcRegistry;
import com.xxl.rpc.admin.core.model.XxlRpcRegistryData;
import com.xxl.rpc.admin.core.model.XxlRpcRegistryMessage;
import com.xxl.rpc.admin.core.result.ReturnT;
import com.xxl.rpc.admin.core.util.JacksonUtil;
import com.xxl.rpc.admin.core.util.PropUtil;
import com.xxl.rpc.admin.dao.IXxlRpcRegistryDao;
import com.xxl.rpc.admin.dao.IXxlRpcRegistryDataDao;
import com.xxl.rpc.admin.dao.IXxlRpcRegistryMessageDao;
import com.xxl.rpc.admin.service.IXxlRpcRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author xuxueli 2016-5-28 15:30:33
 */
@Service
public class XxlRpcRegistryServiceImpl implements IXxlRpcRegistryService, InitializingBean, DisposableBean {
    private static Logger logger = LoggerFactory.getLogger(XxlRpcRegistryServiceImpl.class);


    @Resource
    private IXxlRpcRegistryDao xxlRpcRegistryDao;
    @Resource
    private IXxlRpcRegistryDataDao xxlRpcRegistryDataDao;
    @Resource
    private IXxlRpcRegistryMessageDao xxlRpcRegistryMessageDao;

    @Value("${xxl.rpc.registry.data.filepath}")
    private String registryDataFilePath;
    @Value("${xxl.rpc.registry.accessToken}")
    private String accessToken;

    private int registryBeatTime = 10;


    @Override
    public Map<String, Object> pageList(int start, int length, String biz, String env, String key) {

        // page list
        List<XxlRpcRegistry> list = xxlRpcRegistryDao.pageList(start, length, biz, env, key);
        int list_count = xxlRpcRegistryDao.pageListCount(start, length, biz, env, key);

        // package result
        Map<String, Object> maps = new HashMap<String, Object>();
        maps.put("recordsTotal", list_count);		// 总记录数
        maps.put("recordsFiltered", list_count);	// 过滤后的总记录数
        maps.put("data", list);  					// 分页列表
        return maps;
    }

    @Override
    public ReturnT<String> delete(int id) {
        XxlRpcRegistry xxlRpcRegistry = xxlRpcRegistryDao.loadById(id);
        if (xxlRpcRegistry != null) {
            xxlRpcRegistryDao.delete(id);
            xxlRpcRegistryDataDao.deleteData(xxlRpcRegistry.getBiz(), xxlRpcRegistry.getEnv(), xxlRpcRegistry.getKey());

            // sendRegistryDataUpdateMessage (delete)
            xxlRpcRegistry.setData("");
            sendRegistryDataUpdateMessage(xxlRpcRegistry);
        }

        return ReturnT.SUCCESS;
    }

    /**
     * send RegistryData Update Message
     */
    private void sendRegistryDataUpdateMessage(XxlRpcRegistry xxlRpcRegistry){
        String registryUpdateJson = JacksonUtil.writeValueAsString(xxlRpcRegistry);

        XxlRpcRegistryMessage registryMessage = new XxlRpcRegistryMessage();
        registryMessage.setType(0);
        registryMessage.setData(registryUpdateJson);
        xxlRpcRegistryMessageDao.add(registryMessage);
    }

    @Override
    public ReturnT<String> update(XxlRpcRegistry xxlRpcRegistry) {

        // valid
        if (xxlRpcRegistry.getBiz()==null || xxlRpcRegistry.getBiz().trim().length()<4 || xxlRpcRegistry.getBiz().trim().length()>255) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "业务线格式非法[4~255]");
        }
        if (xxlRpcRegistry.getEnv()==null || xxlRpcRegistry.getEnv().trim().length()<2 || xxlRpcRegistry.getEnv().trim().length()>255 ) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "环境格式非法[2~255]");
        }
        if (xxlRpcRegistry.getKey()==null || xxlRpcRegistry.getKey().trim().length()<4 || xxlRpcRegistry.getKey().trim().length()>255) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "注册Key格式非法[4~255]");
        }
        if (xxlRpcRegistry.getData()==null || xxlRpcRegistry.getData().trim().length()==0) {
            xxlRpcRegistry.setData(JacksonUtil.writeValueAsString(new ArrayList<String>()));
        }
        List<String> valueList = JacksonUtil.readValue(xxlRpcRegistry.getData(), List.class);
        if (valueList == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "注册Value数据格式非法；限制为字符串数组JSON格式，如 [address,address2]");
        }

        // valid exist
        XxlRpcRegistry exist = xxlRpcRegistryDao.loadById(xxlRpcRegistry.getId());
        if (exist == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "ID参数非法");
        }

        // if refresh
        boolean needMessage = !xxlRpcRegistry.getData().equals(exist.getData());

        int ret = xxlRpcRegistryDao.update(xxlRpcRegistry);
        needMessage = ret>0?needMessage:false;

        if (needMessage) {
            // sendRegistryDataUpdateMessage (update)
            sendRegistryDataUpdateMessage(xxlRpcRegistry);
        }

        return ret>0?ReturnT.SUCCESS:ReturnT.FAIL;
    }

    @Override
    public ReturnT<String> add(XxlRpcRegistry xxlRpcRegistry) {

        // valid
        if (xxlRpcRegistry.getBiz()==null || xxlRpcRegistry.getBiz().trim().length()<4 || xxlRpcRegistry.getBiz().trim().length()>255) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "业务线格式非法[4~255]");
        }
        if (xxlRpcRegistry.getEnv()==null || xxlRpcRegistry.getEnv().trim().length()<2 || xxlRpcRegistry.getEnv().trim().length()>255 ) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "环境格式非法[2~255]");
        }
        if (xxlRpcRegistry.getKey()==null || xxlRpcRegistry.getKey().trim().length()<4 || xxlRpcRegistry.getKey().trim().length()>255) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "注册Key格式非法[4~255]");
        }
        if (xxlRpcRegistry.getData()==null || xxlRpcRegistry.getData().trim().length()==0) {
            xxlRpcRegistry.setData(JacksonUtil.writeValueAsString(new ArrayList<String>()));
        }
        List<String> valueList = JacksonUtil.readValue(xxlRpcRegistry.getData(), List.class);
        if (valueList == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "注册Value数据格式非法；限制为字符串数组JSON格式，如 [address,address2]");
        }

        // valid exist
        XxlRpcRegistry exist = xxlRpcRegistryDao.load(xxlRpcRegistry.getBiz(), xxlRpcRegistry.getEnv(), xxlRpcRegistry.getKey());
        if (exist != null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "注册Key请勿重复");
        }

        int ret = xxlRpcRegistryDao.add(xxlRpcRegistry);
        boolean needMessage = ret>0?true:false;

        if (needMessage) {
            // sendRegistryDataUpdateMessage (add)
            sendRegistryDataUpdateMessage(xxlRpcRegistry);
        }

        return ret>0?ReturnT.SUCCESS:ReturnT.FAIL;
    }


    // ------------------------ remote registry ------------------------

    @Override
    public ReturnT<String> registry(String accessToken, String biz, String env, List<XxlRpcRegistryData> registryDataList) {

        // valid
        if (this.accessToken!=null && this.accessToken.trim().length()>0 && !this.accessToken.equals(accessToken)) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "AccessToken Invalid");
        }
        if (biz==null || biz.trim().length()<4 || biz.trim().length()>255) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "Biz Invalid[4~255]");
        }
        if (env==null || env.trim().length()<2 || env.trim().length()>255) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "Env Invalid[2~255]");
        }
        if (registryDataList==null || registryDataList.size()==0) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "Registry DataList Invalid");
        }
        for (XxlRpcRegistryData registryData: registryDataList) {
            if (registryData.getKey()==null || registryData.getKey().trim().length()<4 || registryData.getKey().trim().length()>255) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, "Registry Key Invalid[4~255]");
            }
            if (registryData.getValue()==null || registryData.getValue().trim().length()<4 || registryData.getValue().trim().length()>255) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, "Registry Value Invalid[4~255]");
            }
        }

        // fill + add queue
        for (XxlRpcRegistryData registryData: registryDataList) {
            registryData.setBiz(biz);
            registryData.setEnv(env);
        }
        registryQueue.addAll(registryDataList);

        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> remove(String accessToken, String biz, String env, List<XxlRpcRegistryData> registryDataList) {

        // valid
        if (this.accessToken!=null && this.accessToken.trim().length()>0 && !this.accessToken.equals(accessToken)) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "AccessToken Invalid");
        }
        if (biz==null || biz.trim().length()<4 || biz.trim().length()>255) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "Biz Invalid[4~255]");
        }
        if (env==null || env.trim().length()<2 || env.trim().length()>255) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "Env Invalid[2~255]");
        }
        if (registryDataList==null || registryDataList.size()==0) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "Registry DataList Invalid");
        }
        for (XxlRpcRegistryData registryData: registryDataList) {
            if (registryData.getKey()==null || registryData.getKey().trim().length()<4 || registryData.getKey().trim().length()>255) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, "Registry Key Invalid[4~255]");
            }
            if (registryData.getValue()==null || registryData.getValue().trim().length()<4 || registryData.getValue().trim().length()>255) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, "Registry Value Invalid[4~255]");
            }
        }

        // fill + add queue
        for (XxlRpcRegistryData registryData: registryDataList) {
            registryData.setBiz(biz);
            registryData.setEnv(env);
        }
        removeQueue.addAll(registryDataList);

        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<Map<String, List<String>>> discovery(String accessToken, String biz, String env, List<String> keys) {

        // valid
        if (this.accessToken!=null && this.accessToken.trim().length()>0 && !this.accessToken.equals(accessToken)) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "AccessToken Invalid");
        }
        if (biz==null || biz.trim().length()<2 || biz.trim().length()>255) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "Biz Invalid[2~255]");
        }
        if (env==null || env.trim().length()<2 || env.trim().length()>255) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "Env Invalid[2~255]");
        }
        if (keys==null || keys.size()==0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "keys Invalid.");
        }
        for (String key: keys) {
            if (key==null || key.trim().length()<4 || key.trim().length()>255) {
                return new ReturnT<>(ReturnT.FAIL_CODE, "Key Invalid[4~255]");
            }
        }

        Map<String, List<String>> result = new HashMap<String, List<String>>();
        for (String key: keys) {
            XxlRpcRegistryData xxlRpcRegistryData = new XxlRpcRegistryData();
            xxlRpcRegistryData.setBiz(biz);
            xxlRpcRegistryData.setEnv(env);
            xxlRpcRegistryData.setKey(key);

            List<String> dataList = new ArrayList<String>();
            XxlRpcRegistry fileXxlRpcRegistry = getFileRegistryData(xxlRpcRegistryData);
            if (fileXxlRpcRegistry !=null) {
                dataList = fileXxlRpcRegistry.getDataList();
            }

            result.put(key, dataList);
        }

        return new ReturnT<Map<String, List<String>>>(result);
    }

    @Override
    public DeferredResult<ReturnT<String>> monitor(String accessToken, String biz, String env, List<String> keys) {

        // init
        DeferredResult deferredResult = new DeferredResult(30 * 1000L, new ReturnT<>(ReturnT.SUCCESS_CODE, "Monitor timeout, no key updated."));

        // valid
        if (this.accessToken!=null && this.accessToken.trim().length()>0 && !this.accessToken.equals(accessToken)) {
            deferredResult.setResult(new ReturnT<>(ReturnT.FAIL_CODE, "AccessToken Invalid"));
            return deferredResult;
        }
        if (biz==null || biz.trim().length()<4 || biz.trim().length()>255) {
            deferredResult.setResult(new ReturnT<>(ReturnT.FAIL_CODE, "Biz Invalid[4~255]"));
            return deferredResult;
        }
        if (env==null || env.trim().length()<2 || env.trim().length()>255) {
            deferredResult.setResult(new ReturnT<>(ReturnT.FAIL_CODE, "Env Invalid[2~255]"));
            return deferredResult;
        }
        if (keys==null || keys.size()==0) {
            deferredResult.setResult(new ReturnT<>(ReturnT.FAIL_CODE, "keys Invalid."));
            return deferredResult;
        }
        for (String key: keys) {
            if (key==null || key.trim().length()<4 || key.trim().length()>255) {
                deferredResult.setResult(new ReturnT<>(ReturnT.FAIL_CODE, "Key Invalid[4~255]"));
                return deferredResult;
            }
        }

        // monitor by client
        for (String key: keys) {
            String fileName = parseRegistryDataFileName(biz, env, key);

            List<DeferredResult> deferredResultList = registryDeferredResultMap.get(fileName);
            if (deferredResultList == null) {
                deferredResultList = new ArrayList<>();
                registryDeferredResultMap.put(fileName, deferredResultList);
            }

            deferredResultList.add(deferredResult);
        }

        return deferredResult;
    }

    /**
     * update Registry And Message
     */
    private void checkRegistryDataAndSendMessage(XxlRpcRegistryData xxlRpcRegistryData){
        // data json
        List<XxlRpcRegistryData> xxlRpcRegistryDataList = xxlRpcRegistryDataDao.findData(xxlRpcRegistryData.getBiz(), xxlRpcRegistryData.getEnv(), xxlRpcRegistryData.getKey());
        List<String> valueList = new ArrayList<>();
        if (xxlRpcRegistryDataList !=null && xxlRpcRegistryDataList.size()>0) {
            for (XxlRpcRegistryData dataItem: xxlRpcRegistryDataList) {
                valueList.add(dataItem.getValue());
            }
        }
        String dataJson = JacksonUtil.writeValueAsString(valueList);

        // update registry and message
        XxlRpcRegistry xxlRpcRegistry = xxlRpcRegistryDao.load(xxlRpcRegistryData.getBiz(), xxlRpcRegistryData.getEnv(), xxlRpcRegistryData.getKey());
        boolean needMessage = false;
        if (xxlRpcRegistry == null) {
            xxlRpcRegistry = new XxlRpcRegistry();
            xxlRpcRegistry.setBiz(xxlRpcRegistryData.getBiz());
            xxlRpcRegistry.setEnv(xxlRpcRegistryData.getEnv());
            xxlRpcRegistry.setKey(xxlRpcRegistryData.getKey());
            xxlRpcRegistry.setData(dataJson);
            xxlRpcRegistryDao.add(xxlRpcRegistry);
            needMessage = true;
        } else {

            // check status, locked and disabled not use
            if (xxlRpcRegistry.getStatus() != 0) {
                return;
            }

            if (!xxlRpcRegistry.getData().equals(dataJson)) {
                xxlRpcRegistry.setData(dataJson);
                xxlRpcRegistryDao.update(xxlRpcRegistry);
                needMessage = true;
            }
        }

        if (needMessage) {
            // sendRegistryDataUpdateMessage (registry update)
            sendRegistryDataUpdateMessage(xxlRpcRegistry);
        }

    }

    // ------------------------ broadcase + file data ------------------------

    private ExecutorService executorService = Executors.newCachedThreadPool();
    private volatile boolean executorStoped = false;
    private volatile List<Integer> readedMessageIds = Collections.synchronizedList(new ArrayList<Integer>());

    private volatile LinkedBlockingQueue<XxlRpcRegistryData> registryQueue = new LinkedBlockingQueue<XxlRpcRegistryData>();
    private volatile LinkedBlockingQueue<XxlRpcRegistryData> removeQueue = new LinkedBlockingQueue<XxlRpcRegistryData>();
    private Map<String, List<DeferredResult>> registryDeferredResultMap = new ConcurrentHashMap<>();

    @Override
    public void afterPropertiesSet() throws Exception {

        // valid
        if (registryDataFilePath==null || registryDataFilePath.trim().length()==0) {
            throw new RuntimeException("xxl-rpc, registryDataFilePath empty.");
        }

        /**
         * registry registry data         (client-num/10 s)
         */
        for (int i = 0; i < 10; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    while (!executorStoped) {
                        try {
                            XxlRpcRegistryData xxlRpcRegistryData = registryQueue.take();
                            if (xxlRpcRegistryData !=null) {

                                // refresh or add
                                int ret = xxlRpcRegistryDataDao.refresh(xxlRpcRegistryData);
                                if (ret == 0) {
                                    xxlRpcRegistryDataDao.add(xxlRpcRegistryData);
                                }

                                // valid file status
                                XxlRpcRegistry fileXxlRpcRegistry = getFileRegistryData(xxlRpcRegistryData);
                                if (fileXxlRpcRegistry == null) {
                                    // go on
                                } else if (fileXxlRpcRegistry.getStatus() != 0) {
                                    continue;     // "Status limited."
                                } else {
                                    if (fileXxlRpcRegistry.getDataList().contains(xxlRpcRegistryData.getValue())) {
                                        continue;     // "Repeated limited."
                                    }
                                }

                                // checkRegistryDataAndSendMessage
                                checkRegistryDataAndSendMessage(xxlRpcRegistryData);
                            }
                        } catch (Exception e) {
                            if (!executorStoped) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                    }
                }
            });
        }

        /**
         * remove registry data         (client-num/start-interval s)
         */
        for (int i = 0; i < 10; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    while (!executorStoped) {
                        try {
                            XxlRpcRegistryData xxlRpcRegistryData = removeQueue.take();
                            if (xxlRpcRegistryData != null) {

                                // delete
                                xxlRpcRegistryDataDao.deleteDataValue(xxlRpcRegistryData.getBiz(), xxlRpcRegistryData.getEnv(), xxlRpcRegistryData.getKey(), xxlRpcRegistryData.getValue());

                                // valid file status
                                XxlRpcRegistry fileXxlRpcRegistry = getFileRegistryData(xxlRpcRegistryData);
                                if (fileXxlRpcRegistry == null) {
                                    // go on
                                } else if (fileXxlRpcRegistry.getStatus() != 0) {
                                    continue;   // "Status limited."
                                } else {
                                    if (!fileXxlRpcRegistry.getDataList().contains(xxlRpcRegistryData.getValue())) {
                                        continue;   // "Repeated limited."
                                    }
                                }

                                // checkRegistryDataAndSendMessage
                                checkRegistryDataAndSendMessage(xxlRpcRegistryData);
                            }
                        } catch (Exception e) {
                            if (!executorStoped) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                    }
                }
            });
        }

        /**
         * broadcase new one registry-data-file     (1/1s)
         *
         * clean old message   (1/10s)
         */
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                while (!executorStoped) {
                    try {
                        // new message, filter readed
                        List<XxlRpcRegistryMessage> messageList = xxlRpcRegistryMessageDao.findMessage(readedMessageIds);
                        if (messageList!=null && messageList.size()>0) {
                            for (XxlRpcRegistryMessage message: messageList) {
                                readedMessageIds.add(message.getId());

                                if (message.getType() == 0) {   // from registry、add、update、deelete，ne need sync from db, only write

                                    XxlRpcRegistry xxlRpcRegistry = JacksonUtil.readValue(message.getData(), XxlRpcRegistry.class);

                                    // process data by status
                                    if (xxlRpcRegistry.getStatus() == 1) {
                                        // locked, not updated
                                    } else if (xxlRpcRegistry.getStatus() == 2) {
                                        // disabled, write empty
                                        xxlRpcRegistry.setData(JacksonUtil.writeValueAsString(new ArrayList<String>()));
                                    } else {
                                        // default, sync from db （aready sync before message, only write）
                                    }

                                    // sync file
                                    setFileRegistryData(xxlRpcRegistry);
                                }
                            }
                        }

                        // clean old message;
                        if ( (System.currentTimeMillis()/1000) % registryBeatTime ==0) {
                            xxlRpcRegistryMessageDao.cleanMessage(registryBeatTime);
                            readedMessageIds.clear();
                        }
                    } catch (Exception e) {
                        if (!executorStoped) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (Exception e) {
                        if (!executorStoped) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
            }
        });

        /**
         *  clean old registry-data     (1/10s)
         *
         *  sync total registry-data db + file      (1+N/10s)
         *
         *  clean old registry-data file
         */
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                while (!executorStoped) {

                    // align to beattime
                    try {
                        long sleepSecond = registryBeatTime - (System.currentTimeMillis()/1000)%registryBeatTime;
                        if (sleepSecond>0 && sleepSecond<registryBeatTime) {
                            TimeUnit.SECONDS.sleep(sleepSecond);
                        }
                    } catch (Exception e) {
                        if (!executorStoped) {
                            logger.error(e.getMessage(), e);
                        }
                    }

                    try {
                        // clean old registry-data in db
                        xxlRpcRegistryDataDao.cleanData(registryBeatTime * 3);

                        // sync registry-data, db + file
                        int offset = 0;
                        int pagesize = 1000;
                        List<String> registryDataFileList = new ArrayList<>();

                        List<XxlRpcRegistry> registryList = xxlRpcRegistryDao.pageList(offset, pagesize, null, null, null);
                        while (registryList!=null && registryList.size()>0) {

                            for (XxlRpcRegistry registryItem: registryList) {

                                // process data by status
                                if (registryItem.getStatus() == 1) {
                                    // locked, not updated
                                } else if (registryItem.getStatus() == 2) {
                                    // disabled, write empty
                                    String dataJson = JacksonUtil.writeValueAsString(new ArrayList<String>());
                                    registryItem.setData(dataJson);
                                } else {
                                    // default, sync from db
                                    List<XxlRpcRegistryData> xxlRpcRegistryDataList = xxlRpcRegistryDataDao.findData(registryItem.getBiz(), registryItem.getEnv(), registryItem.getKey());
                                    List<String> valueList = new ArrayList<String>();
                                    if (xxlRpcRegistryDataList !=null && xxlRpcRegistryDataList.size()>0) {
                                        for (XxlRpcRegistryData dataItem: xxlRpcRegistryDataList) {
                                            valueList.add(dataItem.getValue());
                                        }
                                    }
                                    String dataJson = JacksonUtil.writeValueAsString(valueList);

                                    // check update, sync db
                                    if (!registryItem.getData().equals(dataJson)) {
                                        registryItem.setData(dataJson);
                                        xxlRpcRegistryDao.update(registryItem);
                                    }
                                }

                                // sync file
                                String registryDataFile = setFileRegistryData(registryItem);

                                // collect registryDataFile
                                registryDataFileList.add(registryDataFile);
                            }


                            offset += 1000;
                            registryList = xxlRpcRegistryDao.pageList(offset, pagesize, null, null, null);
                        }

                        // clean old registry-data file
                        cleanFileRegistryData(registryDataFileList);

                    } catch (Exception e) {
                        if (!executorStoped) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                    try {
                        TimeUnit.SECONDS.sleep(registryBeatTime);
                    } catch (Exception e) {
                        if (!executorStoped) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
            }
        });


    }

    @Override
    public void destroy() throws Exception {
        executorStoped = true;
        executorService.shutdownNow();
    }


    // ------------------------ file opt ------------------------

    // get
    public XxlRpcRegistry getFileRegistryData(XxlRpcRegistryData xxlRpcRegistryData){

        // fileName
        String fileName = parseRegistryDataFileName(xxlRpcRegistryData.getBiz(), xxlRpcRegistryData.getEnv(), xxlRpcRegistryData.getKey());

        // read
        Properties prop = PropUtil.loadProp(fileName);
        if (prop!=null) {
            XxlRpcRegistry fileXxlRpcRegistry = new XxlRpcRegistry();
            fileXxlRpcRegistry.setData(prop.getProperty("data"));
            fileXxlRpcRegistry.setStatus(Integer.valueOf(prop.getProperty("status")));
            fileXxlRpcRegistry.setDataList(JacksonUtil.readValue(fileXxlRpcRegistry.getData(), List.class));
            return fileXxlRpcRegistry;
        }
        return null;
    }
    private String parseRegistryDataFileName(String biz, String env, String key){
        // fileName
        String fileName = registryDataFilePath
                .concat(File.separator).concat(biz)
                .concat(File.separator).concat(env)
                .concat(File.separator).concat(key)
                .concat(".properties");
        return fileName;
    }

    // set
    public String setFileRegistryData(XxlRpcRegistry xxlRpcRegistry){

        // fileName
        String fileName = parseRegistryDataFileName(xxlRpcRegistry.getBiz(), xxlRpcRegistry.getEnv(), xxlRpcRegistry.getKey());

        // valid repeat update
        Properties existProp = PropUtil.loadProp(fileName);
        if (existProp != null
                && existProp.getProperty("data").equals(xxlRpcRegistry.getData())
                && existProp.getProperty("status").equals(String.valueOf(xxlRpcRegistry.getStatus()))
                ) {
            return new File(fileName).getPath();
        }

        // write
        Properties prop = new Properties();
        prop.setProperty("data", xxlRpcRegistry.getData());
        prop.setProperty("status", String.valueOf(xxlRpcRegistry.getStatus()));

        PropUtil.writeProp(prop, fileName);

        logger.info(">>>>>>>>>>> xxl-rpc, setFileRegistryData: biz={}, env={}, key={}, data={}"
                , xxlRpcRegistry.getBiz(), xxlRpcRegistry.getEnv(), xxlRpcRegistry.getKey(), xxlRpcRegistry.getData());


        // brocast monitor client
        List<DeferredResult> deferredResultList = registryDeferredResultMap.get(fileName);
        if (deferredResultList != null) {
            registryDeferredResultMap.remove(fileName);
            for (DeferredResult deferredResult: deferredResultList) {
                deferredResult.setResult(new ReturnT<>(ReturnT.SUCCESS_CODE, "Monitor key update."));
            }
        }

        return new File(fileName).getPath();
    }
    // clean
    public void cleanFileRegistryData(List<String> registryDataFileList){
        filterChildPath(new File(registryDataFilePath), registryDataFileList);
    }

    public void filterChildPath(File parentPath, final List<String> registryDataFileList){
        if (!parentPath.exists() || parentPath.list()==null || parentPath.list().length==0) {
            return;
        }
        File[] childFileList = parentPath.listFiles();
        for (File childFile: childFileList) {
            if (childFile.isFile() && !registryDataFileList.contains(childFile.getPath())) {
                childFile.delete();

                logger.info(">>>>>>>>>>> xxl-rpc, cleanFileRegistryData, RegistryData Path={}", childFile.getPath());
            }
            if (childFile.isDirectory()) {
                if (parentPath.listFiles()!=null && parentPath.listFiles().length>0) {
                    filterChildPath(childFile, registryDataFileList);
                } else {
                    childFile.delete();
                }

            }
        }

    }

}
