/*
Copyright (c) Uber Technologies, Inc.

This source code is licensed under the MIT license found in the
LICENSE file in the root directory of this source tree.
*/
import React from 'react';

import { StatefulSelect } from '..';

export function Scenario() {
  return (
    <React.Fragment>
      <button>before</button>
      <StatefulSelect
        options={[
          { id: 'a', label: 'hey!' },
          { id: 'b', label: 'are you listening?' },
          { id: 'c', label: 'look at me!' },
        ]}
      />

      <button>after</button>
    </React.Fragment>
  );
}
