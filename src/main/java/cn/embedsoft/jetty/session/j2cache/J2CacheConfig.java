package cn.embedsoft.jetty.session.j2cache;

/**
 * j2cache config
 * 
 * @author gyli@embed-soft.cn
 *
 */
public class J2CacheConfig {
    
    private String configfile;
    private int savePeriodSec = 0;
    
    public int getSavePeriodSec() {
        return savePeriodSec;
    }

    public void setSavePeriodSec(int savePeriodSec) {
        this.savePeriodSec = savePeriodSec;
    }

    public String getConfigfile() {
        return configfile;
    }

    public void setConfigfile(String configfile) {
        this.configfile = configfile;
    }

}
