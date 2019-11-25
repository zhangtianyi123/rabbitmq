package zty.practise.cloudrabbit.util;

import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 一致性hash获取路由
 * 
 * 路由存储可能发生变化，本类为线程不安全的类
 * 
 * @author zhangtianyi
 *
 */
@Component
public class ConsistentHash {

	private static SortedMap<Integer, String> sortedMap = new TreeMap<Integer, String>();

	private static final int VIRTUAL_NODES = 15;
	
	static {
	}
	
	public static void initNode(String exchange, int instanceCount) {
		changeNode(exchange, instanceCount);
	}
	
	public static void changeNode(String exchange, int instanceCount) {
		for (int i = 0; i < instanceCount; i++) {
			String routingKey = exchange + "-" + i;
			for(int j=0; j<VIRTUAL_NODES; j++){
                String virtualNodeName = routingKey + "&&VN" + String.valueOf(j);
                int hash = getHash(virtualNodeName);
                sortedMap.put(hash, virtualNodeName);
            }
		}
	}

    public static String getRoutingKey(String key) {  
        int hash = getHash(key);  
        //得到大于该Hash值的所有Map  
        SortedMap<Integer, String> subMap = sortedMap.tailMap(hash);  
        String virtualNode;
        if(subMap.isEmpty()){  
            //如果没有比该key的hash值大的，则从第一个node开始  
            Integer i = sortedMap.firstKey();  
            virtualNode =  sortedMap.get(i);  
        }else{  
            //第一个Key就是顺时针过去离node最近的那个结点  
            Integer i = subMap.firstKey();  
            virtualNode = subMap.get(i);  
        }  
        
        if(StringUtils.isNotBlank(virtualNode)){
            return virtualNode.substring(0, virtualNode.indexOf("&&"));
        }
        return null;
    }
    
	private static int getHash(String str) {
		final int p = 16777619;
		int hash = (int) 2166136261L;
		for (int i = 0; i < str.length(); i++)
			hash = (hash ^ str.charAt(i)) * p;
		hash += hash << 13;
		hash ^= hash >> 7;
		hash += hash << 3;
		hash ^= hash >> 17;
		hash += hash << 5;
 
		// 如果算出来的值为负数则取其绝对值
		if (hash < 0)
			hash = Math.abs(hash);
		return hash;
	}
}
