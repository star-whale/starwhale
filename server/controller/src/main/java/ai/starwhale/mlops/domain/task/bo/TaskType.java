package ai.starwhale.mlops.domain.task.bo;

public enum TaskType {
    UNKNOWN(-1), PPL(1), CMP(2);
    final int value;
    TaskType(int v){
        this.value = v;
    }
    public int getValue(){
        return this.value;
    }


    public static TaskType from(int v){
        for(TaskType taskType:TaskType.values()){
            if(taskType.value == v){
                return taskType;
            }
        }
        return UNKNOWN;
    }
}
