package com.google.monitoring.runtime.instrumentation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by jmaloney on 5/23/2016.
 */
public class Test {

    private static List<String> stringList = new ArrayList<>();
    private static List<boolean[]> bools = new ArrayList<>();
    private static List<Class[]> classes = new ArrayList<>();
    private static List<Object> objects = new ArrayList<>();
//    private static List<Entity> entities = new ArrayList<>();
    private static List<boolean[][]> nestedBools = new ArrayList<>();

    private static List<Runnable> runnables = new ArrayList<>();

    public static void main(String [] args) throws Exception {
        try {


//        while (true) {
//            stringList.add(ThreadLocalRandom.current().nextLong() + "" + ThreadLocalRandom.current().nextLong() + "da" + ThreadLocalRandom.current().nextLong());
//            if (ThreadLocalRandom.current().nextLong() % 100 == 0) {
//                stringList.clear();
//            }
//        }

            long cnt = 0;
        while (true) {
            runnables.add(() -> runnables.size());
            if (ThreadLocalRandom.current().nextLong() % 100 == 0) {
                runnables.clear();
            }
            cnt++;
            if (ThreadLocalRandom.current().nextLong() % 1000000 == 0) {
                System.out.println("here " + cnt);
            }
        }


//        while (true) {
//            classes.add(new Class[5]);
//            if (ThreadLocalRandom.current().nextLong() % 100 == 0) {
//                classes.clear();
//            }
//        }


//        while (true) {
//            bools.add(new boolean[10]);
//            if (ThreadLocalRandom.current().nextLong() % 100 == 0) {
//                bools.clear();
//            }
//        }


//        List<Class> classList = new ArrayList<>();
//        classList.add(ArrayList.class);
//        classList.add(LinkedList.class);
//        while (true) {
//            objects.add(classList.get(ThreadLocalRandom.current().nextInt(0,2)).newInstance());
//            if (ThreadLocalRandom.current().nextLong() % 100 == 0) {
//                objects.clear();
//            }
//        }

//        Entity e = new Entity();
//        while (true) {
//            entities.add(e.dup());
//            if (ThreadLocalRandom.current().nextLong() % 100 == 0) {
//                entities.clear();
//            }
//        }


//        Class[] classArray = new Class[5];
//        while (true) {
//            classes.add(Arrays.copyOf(classArray, 5));
//            if (ThreadLocalRandom.current().nextLong() % 100 == 0) {
//                classes.clear();
//            }
//        }

//            Constructor<Entity> constructor = Entity.class.getConstructor();
//            while (true) {
//                constructor.newInstance();
//            }


//            while (true) {
//                nestedBools.add(new boolean[5][5]);
//                if (ThreadLocalRandom.current().nextLong() % 100 == 0) {
//                    nestedBools.clear();
//                }
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    private static class Entity implements Cloneable {
//        public Entity() {
//        }
//
//        public Entity dup() {
//            try {
//                return (Entity) clone();
//            } catch (CloneNotSupportedException e) {
//                throw new InternalError();
//            }
//        }
//    }


}
