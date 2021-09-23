package com.wuyuan.database.util;

import java.util.concurrent.atomic.AtomicLong;

public class GetIdUtil {
    private static int count;
    private static AtomicLong id = new AtomicLong(100000);

    private static long aId=100000;

    public static long getId() {

//        String id = HttpClientUtil.doGet(url + tablename);
//        return Long.parseLong(id.trim());
//        GetIdUtil getIdUtil = new GetIdUtil();
//        return getIdUtil.initId();
        return initIds();
    }
    public synchronized static long initIds() {
//        id.incrementAndGet();
        if (aId > 800000) {
            aId = 100000;
        }
        aId++;
        String ids = System.currentTimeMillis() + "";
        ids = ids + aId;
        return Long.parseLong(ids);
    }

    public synchronized long initId() {
        id.incrementAndGet();
        if (id.get() > 800000) {
            id = new AtomicLong(100000);
        }
        String ids = System.currentTimeMillis() + "";
        ids = ids + id.get();
        return Long.parseLong(ids);
    }

    public static void main(String[] args) {
//        System.out.println(id.incrementAndGet());
        for (int i = 0; i < 1000; i++) {
            System.out.println(i);
            new Thread(new Runnable() {
                @Override
                public void run() {
                        System.out.println(initIds());
//                    System.out.println(Thread.currentThread().getId() + ":" + id.addAndGet((int) (Math.random() * 100)));
                }
            }).start();
        }

    }
}
