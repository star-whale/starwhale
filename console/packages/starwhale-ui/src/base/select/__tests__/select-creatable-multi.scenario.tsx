/*
Copyright (c) Uber Technologies, Inc.

This source code is licensed under the MIT license found in the
LICENSE file in the root directory of this source tree.
*/
import * as React from 'react';

import { StatefulSelect } from '..';

export function Scenario() {
  return (
    <StatefulSelect
      creatable
      multi
      options={[{ id: 'Portland', label: 'Portland' }]}
      labelKey="label"
      valueKey="id"
      overrides={{ ValueContainer: { props: { 'data-id': 'selected' } } }}
    />
  );
}
