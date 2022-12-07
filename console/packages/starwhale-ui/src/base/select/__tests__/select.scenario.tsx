/*
Copyright (c) Uber Technologies, Inc.

This source code is licensed under the MIT license found in the
LICENSE file in the root directory of this source tree.
*/
import * as React from 'react';

import { StatefulSelect, SingleSelect, MultiSelect } from '..';

const options = [
  { id: 'AliceBlue', color: '#F0F8FF' },
  { id: 'AntiqueWhite', color: '#FAEBD7' },
  { id: 'Aqua', color: '#00FFFF' },
  { id: 'Aquamarine', color: '#7FFFD4' },
  { id: 'Azure', color: '#F0FFFF' },
  { id: 'Beige', color: '#F5F5DC' },
];

export function Scenario() {
  return (
    <>
      <StatefulSelect
        aria-label="Select a color"
        options={options}
        overrides={{ ValueContainer: { props: { 'data-id': 'selected' } } }}
        labelKey="id"
        valueKey="color"
      />

      <br />
      <StatefulSelect
        aria-label="Select a color"
        options={options}
        overrides={{ ValueContainer: { props: { 'data-id': 'selected' } } }}
        labelKey="id"
        valueKey="color"
        disabled
      />

      <br />
      <StatefulSelect
        isLoading
        aria-label="Select a color"
        options={options}
        labelKey="id"
        valueKey="color"
      />

      <br />
      <StatefulSelect
        isLoading
        aria-label="Select a color"
        options={options}
        labelKey="id"
        valueKey="color"
        disabled
      />

      <br />
      <SingleSelect
        aria-label="Select a color"
        options={options}
        labelKey="id"
        valueKey="color"
        value={[{ color: '#00FFFF' }]}
      />

      <br />
      <SingleSelect
        aria-label="Select a color"
        options={options}
        labelKey="id"
        valueKey="color"
        value={[{ color: '#00FFFF' }]}
        disabled
      />

      <br />
      <MultiSelect
        aria-label="Select a color"
        options={options}
        labelKey="id"
        valueKey="color"
        value={[{ color: '#00FFFF' }]}
      />

      <br />
      <MultiSelect
        aria-label="Select a color"
        options={options}
        labelKey="id"
        valueKey="color"
        value={[{ color: '#00FFFF' }]}
        disabled
      />
    </>
  );
}
