package _设计模式._状态模式._使用环境类实现状态转换;

/**
 * @ClassName: LargerState
 * @Author: lerry_li
 * @CreateDate: 2021/02/23
 * @Description
 */
public class LargerState extends State{
    @Override
    public void display() {
        System.out.println("2倍大小！");
    }
}
