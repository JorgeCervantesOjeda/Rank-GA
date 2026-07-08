// C:/Users/usuario/ownCloud2/RankGA/src/rankga/BatchEvaluatableProblem.java
// Optional problem extension for evaluating a full population in one call.
package rankga;

public interface BatchEvaluatableProblem {

  void evaluateFitnessBatch( Individual[] individuals );
}
