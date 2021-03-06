# synchronized

能够保证在**同一时刻**最多只有**一个**线程执行该段代码，以达到保证并发安全的效果。

# 分类

## 对象锁

包括**方法锁**（默认锁对象为this当前实例对象）和**同步代码块锁**（自己指定锁对象）

### 方法锁形式

synchronized修饰普通方法，锁对象默认为this

```java
    //加锁的xx方法
    private synchronized void method() {
        //...
    }
```

### 代码块形式

手动指定锁对象

```java

    //定义一个锁对象
    Object lock=new Object();

    //xx方法里加锁的代码块
    synchronized(lock){
        //...
    }
```

## 类锁

指synchronized修饰**静态**的方法或指定锁为**Class对象**

- 只有一个Class对象  
Java类可能有很多个对象，但是只有一个Class对象

- 本质  
所谓的类锁，不过是Class对象的锁而已

- 用法和效果  
类锁只能在同一时刻被一个对象拥有

### synchronized加在static方法上

```java
    //加锁的xx静态方法
    private static synchronized void method() {
        //...
    }
```

### synchronized(*.class)代码块

```java
public class A{

    //...

    //xx方法里加锁的代码块
    synchronized(A.class){
        //...
    }

}
```

# 七种常见情况

## 两个线程同时访问**一个对象**的同步方法

由于普通方法的synchronized的加锁对象是this（即当前对象），因此对于同一个对象来说锁对象是相同的，所以这两个线程访问是可以保证同步的。

## 两个线程访问的是**两个对象**的同步方法

两个锁的对象是不同的实例，因此互不干扰，无法保证同步。

## 两个线程访问的是synchronized的**静态**方法

锁对象是相同的（同一个类的Class对象），因此是同步的。

## 同时访问**同步**方法与**非同步**方法

非同步方法不收到影响。

## 访问同一个对象的**不同**的普通同步方法

由于普通方法的synchronized的加锁对象是this（即当前对象），因此对于同一个对象来说锁对象是相同的，所以结果是同步的。

## 同时访问**静态**synchronized和**非静态**synchronized方法

两个锁对象不同，因此不是同步的。

## 方法抛出**异常**后，会**释放锁**

加锁的方法在执行过程中抛出异常，JVM会帮助释放锁。

# 核心思想

1. 一把锁只能同时被一个线程获取，没有拿到锁的线程必须等待；

2. 每个实例都对应有自己的一把锁，不同实例之间互不影响；例外：锁对象是*.class以及synchronized修饰static方法时，所有对象共用一把锁；

3. 无论是方法正常执行完毕或者方法抛出异常，都会释放锁。

# 性质

## 可重入

### 什么是可重入

指的是同一线程的外层函数获得锁之后，内层函数可以直接再次获取该锁。

### 好处

避免死锁、提升封装性

### 粒度

线程而非调用。

- 情况1：证明同一个方法是可重入的

- 情况2：证明可重入不要求是同一个方法

- 情况3：证明可重入不要求是同一个类中的

## 不可中断

一旦这个锁已经被其他线程获得了，如果当前线程想要获取该锁，只能选择等待或者阻塞，直到其他线程释放该锁。如果其他线程不释放锁，那么当前线程只能永远等待。

# 原理

## 加锁和释放锁的原理

### 通过示例说明

`m1()`和`m2()`是等价的

```java
    Lock lock = new ReentrantLock();

    public synchronized void m1() {
        System.out.println("我是synchronized锁");
    }

    public void m2() {
        lock.lock();
        try {
            System.out.println("我是lock锁");
        } finally {
            lock.unlock();
        }
    }
```

### 深入JVM看字节码

#### 概况

synchronize用的锁是java对象头里的一个字段（每一个对象都有一个对象头，对象头可以存储很多信息，其中有一部分就是用来存储synchronize关键字的锁）

#### 细节

当线程访问代码块的时候必须要得到这把锁，退出整个代码块或者抛出异常的时候必须会释放锁，在JVM规范中对于JVM实现原理，已经有了说明，它的进入锁和释放锁是基于moniter对象来实现同步方法和同步代码块的。  
Monditor对象主要是两个指令，一个是Monditorenter（插入到同步代码块开始的位置），和Monditorexit（退出，插入到方法结束的时候和退出的时候），jvm的规范要求每一个enter必须要要有exit和他对应，但是可能一个moniterenter对应多个Monditorexit，退出的时机包括方法结束和抛出异常。每一个对象都有一个Monditor和他关联，并且moniter被持有后，就会处于锁定状态，当线程执行到Monditorenter指令时，会尝试获取这个对象对应的Monditor的所有权，也是尝试获取对象的锁。

##### 详细解读Monditorenter和Monditorexit指令

Monditorenter和Monditorexit在执行的时候会使对象的锁计数加1或者减1，和操作系统中的PV操作（多线程对临界资源的访问）很像，每一个对象都和一个Monditor相关联，一个moditor的locksware只能被一个线程在同一时间获得，一个线程在尝试获得与这个对象关联的Monditor所有权的时候（Monditorenter），只会发生以下三种情况之一

（1）Monditor计数器为0，意味着目前还还没有被获得，这个线程就会立刻获得，然后把计数器加1，之后别人再想进来就会看到信号，知道它已经被其他线程所持有，加1，意味着当前线程是这个Moditor持有者。（成功获得锁）

（2）当前对象拿到锁的所有权，又重入了，就会导致计数器累加，会变成2,3……随着重入的次数而增加（已经有了这把锁再次重入的情况）

（3）Monditor已经被其他线程所持有了，当前线程再次获取就会得到现在无法获取的信号，就会进入阻塞状态，知道Moditor的计数器变为0，才会再次尝试获取这个锁


Monditorexit

作用，释放Monditor（Monditor可以理解为锁）的所有权，前提是已经拥有了锁的所有权,释放的过程就是将Monditor的计数器减1，减完之后变成0就意味着当前线程不在拥有对Monditor的所有权，就是解锁，如果减完之后不是0，意味着刚才是可重入进来的，所以还继续持有这把锁，最终减到0之后，不仅意味着释放锁了，还意味着刚才被阻塞的线程，会再次尝试获取对该把锁的所有权

## 可重入原理

加锁次数计数器

- JVM负责跟踪对象被加锁的次数

- 线程第一次给对象加锁的时候，计数变为1。每当相同的线程在此对象上再次获得锁时，计数+1

- 每次执行完任务时，计数-1。当计数为0时，锁被完全释放

## 可见性原理

线程的本地内存和主内存最终一致？

# 缺陷

## 效率低

- 锁的释放情况少

- 试图获得锁时不能设定超时

- 不能中断一个正在视图获得锁的线程

## 不够灵活（读写锁更灵活）

加锁和释放锁的时机单一，每个锁仅有单一的条件（某个对象）可能是不够的

## 无法知道是否成功获取到锁

# 常见面试问题

## 1. 使用注意点

- 锁对象不能为空

- 作用域不宜过大

- 避免死锁

##　2. 如何选择Lock和synchronized关键字

思路：避免出错...

## 3. 多线程访问同步方法的各种具体情况

前面已介绍7种常见情况

## 4. 多个线程等待同一个synchronized锁的时候，JVM如何选择下一个获取锁的线程？

## 5. 使得同时只有一个线程可以执行，性能较差，有什么办法可以提升性能？

## 6. 如何更灵活地控制锁的获取和释放？

## 7. 什么是锁的升级、降级？什么是JVM里的偏斜锁、轻量级锁、重量级锁？

# 总结

JVM会自动通过使用monitor来加锁和释放锁，保证了同时只有一个线程可以执行指定代码，从而保证了线程安全，同时具有可重入和不可中断的性质。




