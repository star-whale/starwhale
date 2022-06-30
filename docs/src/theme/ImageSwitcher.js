import React from 'react';
import useThemeContext from '@theme/hooks/useThemeContext';

const ImageSwitcher = ({lightImageSrc, darkImageSrc, alt}) => {
  const { isDarkTheme } = useThemeContext();

  return (
    <img src={isDarkTheme ? darkImageSrc : lightImageSrc} alt={alt} className="img-swt" />
  )
}

export default ImageSwitcher;