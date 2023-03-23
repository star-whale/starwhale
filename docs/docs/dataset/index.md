---
title: Starwhale Dataset User Guide
---

Starwhale organizes data into datasets. A Starwhale dataset is composed by data elements. Each element has its own ID that uniquely identifies itself. Starwhale supports the following data elements types:

* Image
* Audio
* Video
* Text
* Binary

Data elements can have annotations that store additional information of data elements. For example, labels are stored in annotations.

You can go throught all data elements in a Starwhale dataset by scanning with Starwhale SDK. Model evaluation in Starwhale is implemented in this way. Another typical scenario is to process all data with a specific model to generate some labels. For more information, see [Scan a Starwhale dataset](scan).

You can view, sort, and filter a Starwhale dataset in the web UI. All element types are supported by the default dataset viewer in Starwhale Server/Cloud. To learn how to do it, See [TODO].

To create/update a dataset, see [Create/update a Starwhale dataset](creation) for details.

Starwhale datasets are versioned. This feature helps you to keep track of all modifications of a dataset. And you can always run your model evaluation by specifying a specific dataset version, regardless of any future changes to the dataset. In this way, model evaluations in Starwhale are stable and comparatable. For more information about versioning in Starwhale, see [Resource versioning in Starwhale](../common/versioning).

The Starwhale Client (SWCLI) provides dataset related commands. For more information, see [SWCLI reference guide](../references/swcli/dataset).
