package com.beingadish.AroundU.Service;

import com.beingadish.AroundU.Models.WorkerModel;

public interface WorkerService {
    WorkerModel registerWorker(WorkerModel registerWorkerModel, String plainPassword);
}
