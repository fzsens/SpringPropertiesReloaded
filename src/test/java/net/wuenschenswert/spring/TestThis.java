package net.wuenschenswert.spring;

/**
 * Created by fzsens on 6/4/17.
 */
public class TestThis {

    public void whoiam(){
        System.out.println(this);
    }

    class InternalClass {
        public void whoisthis(){
            TestThis.this.whoiam();
        }
    }

    public static void main(String[] args) {
        TestThis testThis = new TestThis();
        InternalClass i1 = testThis.new InternalClass();
        InternalClass i2 = testThis.new InternalClass();
        i1.whoisthis();
        i2.whoisthis();
    }
}
